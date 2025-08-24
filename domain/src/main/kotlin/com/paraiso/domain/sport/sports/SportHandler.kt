package com.paraiso.domain.sport.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.sport.data.toEntity
import com.paraiso.domain.users.EventService
import com.paraiso.domain.util.Constants.GAME_PREFIX
import com.paraiso.domain.util.Constants.TEAM_PREFIX
import com.paraiso.domain.util.ServerConfig.autoBuild
import com.paraiso.domain.util.ServerState
import io.klogging.Klogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

class SportHandler(
    private val sportClient: SportClient,
    private val routesApi: RoutesApi,
    private val sportDBs: SportDBs,
    private val eventService: EventService
) : Klogging {
    private var lastSentScoreboard: Scoreboard? = null
    private var lastSentBoxScores = listOf<BoxScore>()

    suspend fun bootJobs(sport: SiteRoute) = coroutineScope {
        launch { getLeague(sport) }
        launch { buildScoreboard(sport) }
        launch { getStandings(sport) }
        launch { getTeams(sport) }
        launch { getLeaders(sport) }
        launch { getTeamLeaders(sport) }
        launch { getRosters(sport) }
        launch { getSchedules(sport) }
    }

    private suspend fun getLeague(sport: SiteRoute) = coroutineScope {
        if (autoBuild) {
            sportClient.getLeague(sport)?.let { leagueRes ->
                sportDBs.leaguesDB.save(listOf(leagueRes))
            }
        }
    }

    private suspend fun getStandings(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportClient.getStandings(sport)?.let { standingsRes ->
                sportDBs.standingsDB.save(listOf(standingsRes))
            }
            delay(12 * 60 * 60 * 1000)
        }
    }

    private suspend fun getTeams(sport: SiteRoute) = coroutineScope {
        if (autoBuild) {
            sportClient.getTeams(sport).let { teamsRes ->
                launch {
                    addTeamRoutes(sport, teamsRes)
                }
                sportDBs.teamsDB.save(teamsRes)
            }
        }
    }

    private suspend fun addTeamRoutes(sport: SiteRoute, teams: List<Team>) {
        teams.map {
            val now = Clock.System.now()
            RouteDetails(
                id = "/s/${sport.name.lowercase()}/t/${it.abbreviation}",
                route = sport,
                modifier = it.abbreviation,
                title = it.displayName,
                userFavorites = emptySet(),
                about = null,
                createdOn = now,
                updatedOn = now
            )
        }.let {
            routesApi.saveRoutes(it)
        }
    }

    private suspend fun getLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportClient.getLeaders(
                    sport, league.activeSeasonYear, league.activeSeasonType
                )?.let { leadersRes ->
                    sportDBs.leadersDB.save(listOf(leadersRes))
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getTeamLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportDBs.teamsDB.findBySport(sport.name).map { team ->
                    async {
                        sportClient.getTeamLeaders(
                            sport, league.activeSeasonYear, league.activeSeasonType, team.teamId
                        )
                    }
                }.awaitAll().filterNotNull().let { leadersRes ->
                    if(leadersRes.isNotEmpty()){
                        sportDBs.leadersDB.save(leadersRes)
                    }
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getSchedules(sport: SiteRoute) = coroutineScope {
        if (autoBuild) {
            val teams = sportDBs.teamsDB.findBySport(sport.name)
            teams.map { it.teamId }.map { teamId ->
                async {
                    sportClient.getSchedule(sport, teamId)
                }
            }.awaitAll().filterNotNull().let { schedulesRes ->
                if (schedulesRes.isNotEmpty()) {
                    sportDBs.schedulesDB.save(schedulesRes.map { it.toEntity() })
                    sportDBs.competitionsDB.save(schedulesRes.flatMap { it.events })
                    addTeamPosts(sport, teams, schedulesRes)
                }
            }
        }
    }

    private fun addTeamPosts(
        sport: SiteRoute,
        teams: List<Team>,
        schedules: List<Schedule>
    ) {
        // add posts for team sport route - separate for focused discussion
        ServerState.posts.putAll(
            schedules.associate {
                teams.find { team -> team.teamId == it.teamId }?.abbreviation to it.events
            }.flatMap { (key, values) ->
                values.map { competition ->
                    "$TEAM_PREFIX${competition.id}-$key" to Post(
                        id = "$TEAM_PREFIX${competition.id}-$key",
                        title = competition.shortName,
                        content = "${competition.date}-${competition.shortName}",
                        type = PostType.GAME,
                        parentId = "/s/${sport.name.lowercase()}/t/$key",
                        rootId = "$TEAM_PREFIX${competition.id}-$key",
                        data = "$TEAM_PREFIX${competition.id}-$key",
                    )
                }
            }
        )
    }

    private suspend fun getRosters(sport: SiteRoute) = coroutineScope {
        if (autoBuild) {
            sportDBs.teamsDB.findBySport(sport.name).map { it.teamId }.map { teamId ->
                async {
                    sportClient.getRoster(sport, teamId)
                }
            }.awaitAll().filterNotNull().let { rostersRes ->
                if (rostersRes.isNotEmpty()) {
                    sportDBs.rostersDB.save(rostersRes.map { it.toEntity() })
                    sportDBs.athletesDB.save(rostersRes.flatMap { it.athletes })
                    sportDBs.coachesDB.save(rostersRes.mapNotNull { it.coach })
                }
            }
        }
    }
    private suspend fun buildScoreboard(sport: SiteRoute) {
        coroutineScope {
            //send full scoreboard on initial startup
            sportClient.getScoreboard(sport)?.let { scoreboard ->
                saveScoreboardAndGetBoxscores(
                    sport,
                    scoreboard,
                    scoreboard.competitions,
                    true,
                    emptyList()
                )
            }
            var delayBoxScore = 1
            while (isActive) {
                delay(10 * 1000)
                //use existing competitions to delay data transfer where possible
                lastSentScoreboard?.competitions?.let { competitions ->
                    val earliestTime = competitions.minOf { Instant.parse(it.date) }
                    val allStates = competitions.map { it.status.state }.toSet()
                    // if current time is beyond the earliest start time start fetching the scoreboard
                    if (Clock.System.now() > earliestTime) {
                        sportClient.getScoreboard(sport)?.let { scoreboard ->
                            val activeCompetitions = competitions.filter { it.status.state == "in" }
                            val inactiveCompetitions = competitions.filter { it.status.state != "in" }
                            saveScoreboardAndGetBoxscores(
                                sport,
                                scoreboard,
                                activeCompetitions,
                                delayBoxScore == 0,
                                inactiveCompetitions.map { it.toString() }
                            )
                            if (!allStates.contains("pre") && !allStates.contains("in") && Clock.System.now() > earliestTime.plus(1.hours)) {
                                // delay an hour if all games ended - will trigger as long as scoreboard is still prev day
                                delay(60 * 60 * 1000)
                            }
                        }
                        // else if current time is before the earliest time, delay until the earliest time
                    } else if (earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0) {
                        delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
                        delayBoxScore = 0
                    } else {
                        delay(1 * 60 * 1000) // delay one minute (game start not always in sync with clock)
                    }
                }
                //delay boxscore for 30 ticks of delay (every 5 minutes)
                if(delayBoxScore == 30) delayBoxScore = 0
                else delayBoxScore++
            }
        }
    }

    private suspend fun saveScoreboardAndGetBoxscores(
        sport: SiteRoute,
        scoreboard: Scoreboard,
        competitions: List<Competition>,
        enableBoxScore: Boolean,
        inactiveCompetitionIds: List<String>,
    ) = coroutineScope {
        if(competitions.isNotEmpty() && scoreboard != lastSentScoreboard){
            sportDBs.scoreboardsDB.save(listOf(scoreboard.toEntity()))
            sportDBs.competitionsDB.save(competitions)
            eventService.publish(
                MessageType.SCOREBOARD.name,
                "$sport:${Json.encodeToString(scoreboard)}"
            )
            if(enableBoxScore) getBoxscores(sport, competitions.map { it.id }, inactiveCompetitionIds)
            if(scoreboard.competitions.map { it.id } != lastSentScoreboard?.competitions?.map { it.id }){
                //generate game posts first time scoreboard is seen
                addGamePosts(sport, scoreboard.competitions)
            }
            lastSentScoreboard = scoreboard
        }
    }

    private suspend fun getBoxscores(
        sport: SiteRoute,
        competitionIds: List<String>,
        inactiveCompetitionIds: List<String>
    ) = coroutineScope {
        competitionIds.map { gameId ->
            async {
                sportClient.getGameStats(sport, gameId)
            }
        }.awaitAll().filterNotNull().let { newBoxScores ->
            //add inactive boxscores
            val allBoxScores = newBoxScores + lastSentBoxScores.filter { inactiveCompetitionIds.contains(it.id) }
            // map result to teams
            if(allBoxScores != lastSentBoxScores) {
                // map result to teams
                sportDBs.boxscoresDB.save(newBoxScores)
                eventService.publish(
                    MessageType.BOX_SCORES.name,
                    "$sport:${Json.encodeToString(allBoxScores)}"
                )
                lastSentBoxScores = allBoxScores
            }
        }
    }

    private fun addGamePosts(
        sport: SiteRoute,
        competitions: List<Competition>
    ) {
        // add posts for base sport route
        ServerState.posts.putAll(
            competitions.associate { competition ->
                "$GAME_PREFIX${competition.id}" to Post(
                    id = "$GAME_PREFIX${competition.id}",
                    title = competition.shortName,
                    content = "${competition.date}-${competition.shortName}",
                    type = PostType.GAME,
                    parentId = sport.name,
                    rootId = "$GAME_PREFIX${competition.id}",
                    data = "${competition.date}-${competition.shortName}",
                )
            }
        )
    }
}

package com.paraiso.domain.sport.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
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
import com.paraiso.domain.util.Constants.SYSTEM
import com.paraiso.domain.util.Constants.TEAM_PREFIX
import com.paraiso.domain.util.ServerConfig.autoBuild
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

class SportHandler(
    private val sportClient: SportClient,
    private val routesApi: RoutesApi,
    private val sportDBs: SportDBs,
    private val eventService: EventService,
    private val postsDB: PostsDB
) : Klogging {
    private var lastSentScoreboard = ConcurrentHashMap<SiteRoute, Scoreboard>()
    private var lastSentBoxScores = listOf<BoxScore>()

    suspend fun bootJobs(sport: SiteRoute) = coroutineScope {
        launch { buildLeague(sport, manual = false) }
        launch { buildScoreboard(sport) }
        launch { buildStandings(sport) }
        launch { buildTeams(sport, manual = false) }
        launch { buildLeaders(sport) }
        launch { buildTeamLeaders(sport) }
        launch { buildRosters(sport, manual = false) }
        launch { buildSchedules(sport, manual = false) }
    }

    suspend fun buildLeague(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
            sportClient.getLeague(sport)?.let { leagueRes ->
                sportDBs.leaguesDB.save(listOf(leagueRes))
            }
        }
    }

    private suspend fun buildStandings(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportClient.getStandings(sport)?.let { standingsRes ->
                sportDBs.standingsDB.save(listOf(standingsRes))
            }
            delay(12 * 60 * 60 * 1000)
        }
    }

    suspend fun buildTeams(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
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
                userFavorites = 0,
                about = null,
                createdOn = now,
                updatedOn = now
            )
        }.let {
            routesApi.saveRoutes(it)
        }
    }

    private suspend fun buildLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportClient.getLeaders(
                    sport,
                    league.activeSeasonYear,
                    league.activeSeasonType
                )?.let { leadersRes ->
                    sportDBs.leadersDB.save(listOf(leadersRes))
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun buildTeamLeaders(sport: SiteRoute) = coroutineScope {
        while (isActive) {
            sportDBs.leaguesDB.findBySport(sport.name)?.let { league ->
                sportDBs.teamsDB.findBySport(sport.name).map { team ->
                    async {
                        sportClient.getTeamLeaders(
                            sport,
                            league.activeSeasonYear,
                            league.activeSeasonType,
                            team.teamId
                        )
                    }
                }.awaitAll().filterNotNull().let { leadersRes ->
                    if (leadersRes.isNotEmpty()) {
                        sportDBs.leadersDB.save(leadersRes)
                    }
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    suspend fun buildSchedules(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
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

    private suspend fun addTeamPosts(
        sport: SiteRoute,
        teams: List<Team>,
        schedules: List<Schedule>
    ) {
        // add posts for team sport route - separate for focused discussion
        postsDB.save(
            schedules.associate {
                teams.find { team -> team.teamId == it.teamId }?.abbreviation to it.events
            }.flatMap { (key, values) ->
                values.map { competition ->
                    Post(
                        id = "$TEAM_PREFIX$key-${competition.id}",
                        userId = SYSTEM,
                        title = competition.shortName,
                        content = "${competition.date}||${competition.shortName}",
                        type = PostType.EVENT,
                        parentId = "/s/${sport.name.lowercase()}/t/$key",
                        rootId = "$TEAM_PREFIX$key-${competition.id}",
                        data = sport.name,
                        createdOn = competition.date,
                        updatedOn = competition.date
                    )
                }
            }
        )
    }

    suspend fun buildRosters(sport: SiteRoute, manual: Boolean) = coroutineScope {
        if (autoBuild || manual) {
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
            var delayBoxScore = 0
            while (isActive) {
                sportClient.getScoreboard(sport)?.let { scoreboard ->
                    // if completely new scoreboard save it and generate game posts
                    if (scoreboard.competitions.map { it.id }.toSet() != lastSentScoreboard[sport]?.competitions?.map { it.id }?.toSet()) {
                        saveScoreboardAndGetBoxscores(
                            sport,
                            scoreboard,
                            scoreboard.competitions,
                            true,
                            emptyList()
                        )
                        addGamePosts(sport, scoreboard.competitions)
                    } else {
                        // grab earliest game's start time and state of all games
                        val earliestTime = scoreboard.competitions.minOf { it.date ?: Instant.DISTANT_PAST }
                        val allStates = scoreboard.competitions.map { it.status.state }.toSet()
                        // if some games are past the earliest start time update scoreboard and box scores
                        if (Clock.System.now() > earliestTime) {
                            saveScoreboardAndGetBoxscores(
                                sport,
                                scoreboard,
                                scoreboard.competitions.filter { it.status.state == "in" },
                                delayBoxScore == 0,
                                scoreboard.competitions.filter { it.status.state != "in" }
                            )
                            // delay an hour if all games ended - will trigger as long as scoreboard is still prev day
                            if (
                                !allStates.contains("pre") &&
                                !allStates.contains("in") &&
                                Clock.System.now() > earliestTime.plus(1.hours)
                            ) {
                                delay(60 * 60 * 1000)
                            }
                            // else if current time is before the earliest time, delay until the earliest time
                        } else if (earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0) {
                            delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
                            delayBoxScore = 0
                        }
                    }
                }
                // retrieve scoreboard every ten seconds
                delay(10 * 1000)
                // delay boxscore fetch for 30 ticks of delay (every 5 minutes)
                if (delayBoxScore == 30) {
                    delayBoxScore = 0
                } else {
                    delayBoxScore++
                }
            }
        }
    }

    private suspend fun saveScoreboardAndGetBoxscores(
        sport: SiteRoute,
        scoreboard: Scoreboard,
        activeCompetitions: List<Competition>,
        enableBoxScore: Boolean,
        inactiveCompetitions: List<Competition>
    ) = coroutineScope {
        if (scoreboard != lastSentScoreboard[sport]) {
            sportDBs.scoreboardsDB.save(listOf(scoreboard.toEntity()))
            //save inactive comps one last time to ensure all stats are picked up
            if(activeCompetitions.isEmpty()){
                sportDBs.competitionsDB.save(inactiveCompetitions)
                if (enableBoxScore){
                    buildBoxscores(
                        sport,
                        inactiveCompetitions.map { it.id },
                        emptyList()
                    )
                }
            }else{
                sportDBs.competitionsDB.save(activeCompetitions)
                if (enableBoxScore){
                    buildBoxscores(
                        sport,
                        activeCompetitions.map { it.id },
                        inactiveCompetitions.map { it.id }
                    )
                }
            }
            eventService.publish(
                MessageType.SCOREBOARD.name,
                "$sport:${Json.encodeToString(scoreboard)}"
            )
            lastSentScoreboard[sport] = scoreboard
        }
    }

    private suspend fun buildBoxscores(
        sport: SiteRoute,
        competitionIds: List<String>,
        inactiveCompetitionIds: List<String>
    ) = coroutineScope {
        competitionIds.map { gameId ->
            async {
                sportClient.getGameStats(sport, gameId)
            }
        }.awaitAll().filterNotNull().let { newBoxScores ->
            // add inactive boxscores
            val allBoxScores = newBoxScores + lastSentBoxScores.filter { inactiveCompetitionIds.contains(it.id) }
            // map result to teams
            if (allBoxScores != lastSentBoxScores) {
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

    private suspend fun addGamePosts(
        sport: SiteRoute,
        competitions: List<Competition>
    ) {
        // add posts for base sport route
        postsDB.save(
            competitions.map { competition ->
                Post(
                    id = "$GAME_PREFIX${competition.id}",
                    userId = SYSTEM,
                    title = competition.shortName,
                    content = "${competition.date}||${competition.shortName}",
                    type = PostType.EVENT,
                    parentId = "/${sport.name}",
                    rootId = "$GAME_PREFIX${competition.id}",
                    data = sport.name,
                    createdOn = competition.date,
                    updatedOn = competition.date
                )
            }
        )
    }

    suspend fun fillCompetitionData(
        sport: SiteRoute,
        dates: String
    ) {
        // add posts for base sport route
        sportClient.getScoreboardWithDate(sport, dates)?.competitions?.let { competitions ->
            sportDBs.competitionsDB.save(competitions)
            competitions.mapNotNull { competition ->
                sportClient.getGameStats(sport, competition.id)
            }.let { boxScores ->
                sportDBs.boxscoresDB.save(boxScores)
            }
        }
    }
}

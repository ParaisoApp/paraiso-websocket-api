package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.sport.data.toEntity
import com.paraiso.domain.sport.sports.SportDBs
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

class BBallHandler(
    private val bBallOperation: BBallOperation,
    private val routesApi: RoutesApi,
    private val sportDBs: SportDBs,
    private val eventService: EventService
) : Klogging {

    suspend fun bootJobs() = coroutineScope {
        launch { buildScoreboard() }
        launch { getStandings() }
        launch { getTeams() }
        launch { getLeaders() }
        launch { getRosters() }
        launch { getSchedules() }
    }

    private suspend fun getStandings() = coroutineScope {
        while (isActive) {
            bBallOperation.getStandings().also { standingsRes ->
                if (standingsRes != null) {
                    sportDBs.standingsDBAdapter.save(listOf(standingsRes))
                }
            }
            delay(12 * 60 * 60 * 1000)
        }
    }

    private suspend fun getTeams() = coroutineScope {
        if (autoBuild) {
            bBallOperation.getTeams().also { teamsRes ->
                launch {
                    addTeamRoutes(teamsRes)
                }
                sportDBs.teamsDBAdapter.save(teamsRes)
            }
        }
    }

    private suspend fun addTeamRoutes(teams: List<Team>) {
        teams.map {
            val now = Clock.System.now()
            RouteDetails(
                id = "/s/basketball/t/${it.abbreviation}",
                route = SiteRoute.BASKETBALL,
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

    private suspend fun getLeaders() = coroutineScope {
        while (isActive) {
            bBallOperation.getLeaders()?.let { leadersRes ->
                sportDBs.leadersDBAdapter.save(listOf(leadersRes))
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getSchedules() = coroutineScope {
        if (autoBuild) {
            val teams = sportDBs.teamsDBAdapter.findBySport(SiteRoute.BASKETBALL)
            teams.map { it.teamId }.map { teamId ->
                async {
                    bBallOperation.getSchedule(teamId)
                }
            }.awaitAll().filterNotNull().also { schedulesRes ->
                if (schedulesRes.isNotEmpty()) {
                    sportDBs.schedulesDBAdapter.save(schedulesRes.map { it.toEntity() })
                    sportDBs.competitionsDBAdapter.save(schedulesRes.flatMap { it.events })
                    addGamePosts(teams, schedulesRes)
                }
            }
        }
    }

    private fun addGamePosts(
        teams: List<Team>,
        schedules: List<Schedule>
    ) {
        if (
            !ServerState.posts.map { it.key }
                .contains(schedules.firstOrNull()?.events?.firstOrNull()?.id)
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
                            parentId = "/s/${SiteRoute.BASKETBALL}/t/$key",
                            rootId = "$TEAM_PREFIX${competition.id}-$key",
                            data = "$TEAM_PREFIX${competition.id}-$key",
                        )
                    }
                }
            )
            // add posts for base sport route
            ServerState.posts.putAll(
                schedules.flatMap { it.events }.toSet().associate { competition ->
                    "$GAME_PREFIX${competition.id}" to Post(
                        id = "$GAME_PREFIX${competition.id}",
                        title = competition.shortName,
                        content = "${competition.date}-${competition.shortName}",
                        type = PostType.GAME,
                        parentId = SiteRoute.BASKETBALL.name,
                        rootId = "$GAME_PREFIX${competition.id}",
                        data = "${competition.date}-${competition.shortName}",
                    )
                }
            )
        }
    }

    private suspend fun getRosters() = coroutineScope {
        if (autoBuild) {
            sportDBs.teamsDBAdapter.findBySport(SiteRoute.BASKETBALL).map { it.teamId }.map { teamId ->
                async {
                    bBallOperation.getRoster(teamId)
                }
            }.awaitAll().filterNotNull().also { rostersRes ->
                if (rostersRes.isNotEmpty()) {
                    sportDBs.rostersDBAdapter.save(rostersRes.map { it.toEntity() })
                    sportDBs.athletesDBAdapter.save(rostersRes.flatMap { it.athletes })
                    sportDBs.coachesDBAdapter.save(rostersRes.mapNotNull { it.coach })
                }
            }
        }
    }
    private suspend fun buildScoreboard() {
        coroutineScope {
            bBallOperation.getScoreboard()?.let { scoreboard ->
                saveScoreboardAndGetBoxscores(scoreboard, scoreboard.competitions, true)
            }
            var delayBoxScore = 1
            while (isActive) {
                delay(10 * 1000)
                sportDBs.scoreboardsDBAdapter.findById(SiteRoute.BASKETBALL.toString())?.competitions?.let { competitionIds ->
                    sportDBs.competitionsDBAdapter.findByIdIn(competitionIds).let { competitions ->
                        val earliestTime = competitions.minOf { Instant.parse(it.date) }
                        val allStates = competitions.map { it.status.state }.toSet()
                        // if current time is beyond the earliest start time start fetching the scoreboard
                        if (Clock.System.now() > earliestTime) {
                            bBallOperation.getScoreboard()?.let { scoreboard ->
                                val activeCompetitions = competitions.filter { it.status.state == "in" }
                                saveScoreboardAndGetBoxscores(scoreboard, activeCompetitions, delayBoxScore == 0)
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
                }
                //delay boxscore for 30 ticks of delay (every 5 minutes)
                if(delayBoxScore == 30) delayBoxScore = 0
                else delayBoxScore++
            }
        }
    }

    private suspend fun saveScoreboardAndGetBoxscores(
        scoreboard: Scoreboard,
        competitions: List<Competition>,
        enableBoxScore: Boolean
    ) = coroutineScope {
        if(competitions.isNotEmpty()){
            sportDBs.scoreboardsDBAdapter.save(listOf(scoreboard.toEntity()))
            sportDBs.competitionsDBAdapter.save(competitions)
            eventService.publish(
                MessageType.SCOREBOARD.name,
                "${SiteRoute.BASKETBALL}:${Json.encodeToString(scoreboard)}"
            )
            if(enableBoxScore) getBoxscores(competitions.map { it.id })
        }
    }

    private suspend fun getBoxscores(gameIds: List<String>) = coroutineScope {
        gameIds.map { gameId ->
            async {
                bBallOperation.getGameStats(gameId)
            }
        }.awaitAll().filterNotNull().also { newBoxScores ->
            // map result to teams
            sportDBs.boxscoresDBAdapter.save(newBoxScores)
            eventService.publish(
                MessageType.BOX_SCORES.name,
                "${SiteRoute.BASKETBALL}:${Json.encodeToString(newBoxScores)}"
            )
        }
    }
}

package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.util.Constants.GAME_PREFIX
import com.paraiso.domain.util.Constants.TEAM_PREFIX
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
import kotlin.time.Duration.Companion.hours

class BBallHandler(private val bBallOperation: BBallOperation) : Klogging {

    suspend fun bootJobs() = coroutineScope {
        launch { buildScoreboard() }
        launch { getStandings() }
        launch { getTeams() }
        launch { getLeaders() }
    }

    private suspend fun getStandings() = coroutineScope {
        while (isActive) {
            bBallOperation.getStandings().also { standingsRes ->
                if (standingsRes != BBallState.standings) BBallState.standings = standingsRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getTeams() = coroutineScope {
        bBallOperation.getTeams().also { teamsRes ->
            teamsRes.map { it.id }.let { teamIds ->
                launch { getRosters(teamIds) }
                launch { getSchedules(teamIds) }
            }
            launch {
                teamsRes.forEach { addTeamRoute(it) }
            }
        }
        while (isActive) {
            bBallOperation.getTeams().also { teamsRes ->
                if (teamsRes != BBallState.teams) BBallState.teams = teamsRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private fun addTeamRoute(team: Team) {
        val teamRouteId = "${SiteRoute.BASKETBALL}-${SiteRoute.TEAM}-${team.id}"
        ServerState.routes[teamRouteId] = RouteDetails(
            id = teamRouteId,
            route = SiteRoute.BASKETBALL,
            modifier = team.abbreviation,
            title = team.displayName,
            userFavorites = emptySet(),
            about = null,
        )
    }

    private suspend fun getLeaders() = coroutineScope {
        while (isActive) {
            bBallOperation.getLeaders().also { leadersRes ->
                if (leadersRes != BBallState.leaders) BBallState.leaders = leadersRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getSchedules(teamIds: List<String>) = coroutineScope {
        while (isActive) {
            teamIds.map { teamId ->
                async {
                    bBallOperation.getSchedule(teamId)
                }
            }.awaitAll().filterNotNull().also { schedulesRes ->
                if (schedulesRes != BBallState.schedules) BBallState.schedules = schedulesRes
                if (
                    !ServerState.posts.map { it.key }
                        .contains(schedulesRes.firstOrNull()?.events?.firstOrNull()?.id)
                ) {
                    ServerState.posts.putAll(
                        schedulesRes.associate { it.team.abbreviation to it.events }
                            .flatMap { (key, values) ->
                                values.map { competition ->
                                    "$TEAM_PREFIX${competition.id}-${key}" to Post(
                                        id = "$TEAM_PREFIX${competition.id}-${key}",
                                        userId = null,
                                        title = competition.shortName,
                                        content = "${competition.date}-${competition.shortName}",
                                        type = PostType.GAME,
                                        media = null,
                                        votes = emptyMap(),
                                        parentId = "/s/${SiteRoute.BASKETBALL}/t/$key",
                                        rootId = "$TEAM_PREFIX${competition.id}-${key}",
                                        status = PostStatus.ACTIVE,
                                        data = "$TEAM_PREFIX${competition.id}-${key}",
                                        subPosts = mutableSetOf(),
                                        count = 0,
                                        route = null,
                                        createdOn = Clock.System.now(),
                                        updatedOn = Clock.System.now()
                                    )
                                }
                            }
                    )
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getRosters(teamIds: List<String>) = coroutineScope {
        while (isActive) {
            teamIds.map { teamId ->
                async {
                    bBallOperation.getRoster(teamId)
                }
            }.awaitAll().filterNotNull().also { rostersRes ->
                if (rostersRes != BBallState.rosters) BBallState.rosters = rostersRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }
    private suspend fun buildScoreboard() {
        coroutineScope {
            bBallOperation.getScoreboard()?.let { scoreboard ->
                fillGamePosts(scoreboard)
                BBallState.scoreboard = scoreboard
                BBallState.scoreboard?.competitions?.map { it.id }?.let { gameIds ->
                    fetchAndMapGames(gameIds)
                }
            }
            while (isActive) {
                delay(10 * 1000)
                BBallState.scoreboard?.let { sb ->
                    sb.competitions.map { Triple(it.status.state, it.date, it.id) }.let { games ->
                        val earliestTime = games.minOf { Instant.parse(it.second) }
                        val allStates = games.map { it.first }.toSet()
                        // if current time is beyond the earliest start time start fetching the scoreboard
                        if (Clock.System.now() > earliestTime) {
                            bBallOperation.getScoreboard()?.let { scoreboard ->
                                fillGamePosts(scoreboard)
                                BBallState.scoreboard = scoreboard

                                // If boxscores already filled once then filter out games not in progress
//                            TODO DISABLE BOX SCORE UPDATES FOR NOW
//                            games.filter { it.second == "in" }.map { it.third }.let {gameIds ->
//                                fetchAndMapGames(gameIds)
//                            }
                                if (!allStates.contains("pre") && !allStates.contains("in") && Clock.System.now() > earliestTime.plus(1.hours)) {
                                    // delay an hour if all games ended - will trigger as long as scoreboard is still prev day
                                    delay(60 * 60 * 1000)
                                }
                            }
                            // else if current time is before the earliest time, delay until the earliest time
                        } else if (earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0) {
                            delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
                        } else {
                            delay(1 * 60 * 1000) // delay one minute (game start not always in sync with clock)
                        }
                    }
                }
            }
        }
    }

    private suspend fun fillGamePosts(scoreboard: Scoreboard) = coroutineScope {
        if (BBallState.scoreboard?.competitions?.map { it.id } != scoreboard.competitions.map { it.id }) {
            ServerState.posts.putAll(
                scoreboard.competitions.associate { competition ->
                    "$GAME_PREFIX${competition.id}" to Post(
                        id = "$GAME_PREFIX${competition.id}",
                        userId = null,
                        title = competition.shortName,
                        content = "${competition.date}-${competition.shortName}",
                        type = PostType.GAME,
                        media = null,
                        votes = emptyMap(),
                        parentId = SiteRoute.BASKETBALL.name,
                        rootId = "$GAME_PREFIX${competition.id}",
                        status = PostStatus.ACTIVE,
                        data = "${competition.date}-${competition.shortName}",
                        subPosts = mutableSetOf(),
                        count = 0,
                        route = null,
                        createdOn = Clock.System.now(),
                        updatedOn = Clock.System.now()
                    )
                }
            )
        }
    }

    private suspend fun fetchAndMapGames(gameIds: List<String>) = coroutineScope {
        gameIds.map { gameId ->
            async {
                bBallOperation.getGameStats(gameId)
            }
        }.awaitAll().filterNotNull().also { newBoxScores ->
            // map result to teams
            BBallState.boxScores = newBoxScores.flatMap { it.teams }
        }
    }
}

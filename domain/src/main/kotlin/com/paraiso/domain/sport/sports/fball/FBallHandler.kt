package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.sport.data.Scoreboard
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

class FBallHandler(private val fBallOperation: FBallOperation) : Klogging {

    suspend fun bootJobs() = coroutineScope {
        launch { buildScoreboard() }
        launch { getStandings() }
        launch { getTeams() }
        launch { getLeaders() }
    }

    private suspend fun getStandings() = coroutineScope {
        while (isActive) {
            fBallOperation.getStandings().also { standingsRes ->
                if (standingsRes != FBallState.standings) FBallState.standings = standingsRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getTeams() = coroutineScope {
        fBallOperation.getTeams().also { teamsRes ->
            teamsRes.map { it.id }.let { teamIds ->
                launch { getRosters(teamIds) }
                launch { getSchedules(teamIds) }
            }
        }
        while (isActive) {
            fBallOperation.getTeams().also { teamsRes ->
                if (teamsRes != FBallState.teams) FBallState.teams = teamsRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }
    private suspend fun getLeaders() = coroutineScope {
        while (isActive) {
            fBallOperation.getLeaders().also { leadersRes ->
                if (leadersRes != FBallState.leaders) FBallState.leaders = leadersRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getSchedules(teamIds: List<String>) = coroutineScope {
        while (isActive) {
            teamIds.map { teamId ->
                async {
                    fBallOperation.getSchedule(teamId)
                }
            }.awaitAll().filterNotNull().also { schedulesRes ->
                if (schedulesRes != FBallState.schedules) FBallState.schedules = schedulesRes
                if (
                    !ServerState.posts.map { it.key }
                        .contains(schedulesRes.firstOrNull()?.events?.firstOrNull()?.id)
                ) {
                    ServerState.posts.putAll(
                        schedulesRes.associate { it.team.abbreviation to it.events }
                            .flatMap { (key, values) ->
                                values.map { competition ->
                                    "$TEAM_PREFIX${competition.id}-$key" to Post(
                                        id = "$TEAM_PREFIX${competition.id}-$key",
                                        userId = null,
                                        title = competition.shortName,
                                        content = "${competition.date}-${competition.shortName}",
                                        type = PostType.GAME,
                                        media = null,
                                        votes = emptyMap(),
                                        parentId = "/s/${SiteRoute.FOOTBALL}/t/$key",
                                        rootId = "$TEAM_PREFIX${competition.id}-$key",
                                        status = PostStatus.ACTIVE,
                                        data = "TEAM-$key",
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
                    fBallOperation.getRoster(teamId)
                }
            }.awaitAll().filterNotNull().also { rostersRes ->
                if (rostersRes != FBallState.rosters) FBallState.rosters = rostersRes
            }
            delay(6 * 60 * 60 * 1000)
        }
    }
    private suspend fun buildScoreboard() {
        coroutineScope {
            fBallOperation.getScoreboard()?.let { scoreboard ->
                fillGamePosts(scoreboard)
                FBallState.scoreboard = scoreboard
                FBallState.scoreboard?.competitions?.map { it.id }?.let { gameIds ->
                    fetchAndMapGames(gameIds)
                }
            }
            while (isActive) {
                delay(10 * 1000)
                FBallState.scoreboard?.let { sb ->
                    sb.competitions.map { Triple(it.status.state, it.date, it.id) }.let { games ->
                        val earliestTime = games.minOf { Instant.parse(it.second) }
                        val allStates = games.map { it.first }.toSet()
                        // if current time is beyond the earliest start time start fetching the scoreboard
                        if (Clock.System.now() > earliestTime) {
                            fBallOperation.getScoreboard()?.let { scoreboard ->
                                fillGamePosts(scoreboard)
                                FBallState.scoreboard = scoreboard

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
        if (FBallState.scoreboard?.competitions?.map { it.id } != scoreboard.competitions.map { it.id }) {
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
                        parentId = SiteRoute.FOOTBALL.name,
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
                fBallOperation.getGameStats(gameId)
            }
        }.awaitAll().filterNotNull().also { newBoxScores ->
            // map result to teams
            FBallState.boxScores = newBoxScores.flatMap { it.teams }
        }
    }
}

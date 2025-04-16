package com.paraiso.domain.sport

import com.paraiso.domain.sport.sports.AllStandings
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Roster
import com.paraiso.domain.sport.sports.Schedule
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.sport.sports.StatLeaders
import com.paraiso.domain.sport.sports.Team
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

class SportHandler(private val sportOperation: SportOperation) : Klogging {

    suspend fun getStandings() = coroutineScope {
        while (isActive) {
            SportState.standings = sportOperation.getStandings()
            delay(6 * 60 * 60 * 1000)
        }
    }

    suspend fun getTeams() = coroutineScope {
        while (isActive) {
            sportOperation.getTeams().let { teamsRes ->
                SportState.teams = teamsRes
                teamsRes.map { it.id }.let { teamIds ->
                    launch { getRosters(teamIds) }
                    launch { getSchedules(teamIds) }
                }
            }
            delay(6 * 60 * 60 * 1000)
        }
    }
    suspend fun getLeaders() = coroutineScope {
        while (isActive) {
            SportState.leaders = sportOperation.getLeaders()
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getSchedules(teamIds: List<String>) = coroutineScope {
        while (isActive) {
            SportState.schedules = teamIds.map { teamId ->
                async {
                    sportOperation.getSchedule(teamId)
                }
            }.awaitAll().filterNotNull()
            delay(6 * 60 * 60 * 1000)
        }
    }

    private suspend fun getRosters(teamIds: List<String>) = coroutineScope {
        while (isActive) {
            SportState.rosters = teamIds.map { teamId ->
                async {
                    sportOperation.getRoster(teamId)
                }
            }.awaitAll().filterNotNull()
            delay(6 * 60 * 60 * 1000)
        }
    }
    suspend fun buildScoreboard() {
        coroutineScope {
            SportState.scoreboard = sportOperation.getScoreboard()
            SportState.scoreboard?.competitions?.map { it.id }?.let { gameIds ->
                fetchAndMapGames(gameIds)
            }
            while (isActive) {
                delay(10 * 1000)
                SportState.scoreboard?.let { sb ->
                    sb.competitions.map { Triple(it.status.state, it.date, it.id) }.let { games ->
                        val earliestTime = games.minOf { Instant.parse(it.second) }
                        val allStates = games.map { it.first }.toSet()
                        // if current time is beyond the earliest start time start fetching the scoreboard
                        if (Clock.System.now() > earliestTime) {
                            SportState.scoreboard = sportOperation.getScoreboard()

                            // If boxscores already filled once then filter out games not in progress
//                            DISABLE BOX SCORE UPDATES FOR NOW
//                            games.filter { it.second == "in" }.map { it.third }.let {gameIds ->
//                                fetchAndMapGames(gameIds)
//                            }
                            if (!allStates.contains("pre") && !allStates.contains("in") && Clock.System.now() > earliestTime.plus(1.hours)) {
                                // delay an hour if all games ended - will trigger as long as scoreboard is still prev day
                                delay(60 * 60 * 1000)
                            }
                            // else if current time is before the earliest time, delay until the earliest time
                        } else if (earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() > 0) {
                            delay(earliestTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchAndMapGames(gameIds: List<String>) = coroutineScope {
        gameIds.map { gameId ->
            async {
                sportOperation.getGameStats(gameId)
            }
        }.awaitAll().filterNotNull().also { newBoxScores ->
            // map result to teams
            SportState.boxScores = newBoxScores.flatMap { it.teams }
        }
    }
}

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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

class SportHandler(private val sportOperation: SportOperation) : Klogging {
    var scoreboard: Scoreboard? = null
    var teams: List<Team> = emptyList()
    var standings: AllStandings? = null
    var boxScores: List<FullTeam> = emptyList()
    var rosters: List<Roster> = emptyList()
    var leaders: StatLeaders? = null
    var schedules: List<Schedule> = emptyList()

    suspend fun getStandings() {
        standings = sportOperation.getStandings()
    }

    suspend fun getTeams() {
        sportOperation.getTeams().let { teamsRes ->
            teams = teamsRes
            getRosters(teamsRes)
            coroutineScope {
                teamsRes.map { team ->
                    async {
                        sportOperation.getSchedule(team.id)
                    }
                }.awaitAll().filterNotNull().let{scheduleResults ->
                    schedules = scheduleResults
                }
            }
        }
    }
    suspend fun getLeaders() {
        leaders = sportOperation.getLeaders()
    }

    private suspend fun getRosters(teamsRes: List<Team>) = coroutineScope {
        rosters = teamsRes.map { team ->
            async {
                sportOperation.getRoster(team.id)
            }
        }.awaitAll().filterNotNull()
    }
    suspend fun buildScoreboard() {
        coroutineScope {
            scoreboard = sportOperation.getScoreboard()
            scoreboard?.competitions?.map { it.id }?.let { gameIds ->
                fetchAndMapGames(gameIds)
            }
            while (isActive) {
                delay(10 * 1000)
                scoreboard?.let { sb ->
                    sb.competitions.map { Triple(it.status.state, it.date, it.id) }.let { games ->
                        val earliestTime = games.minOf { Instant.parse(it.second) }
                        val allStates = games.map { it.first }.toSet()
                        // if current time is beyond the earliest start time start fetching the scoreboard
                        if (Clock.System.now() > earliestTime) {
                            scoreboard = sportOperation.getScoreboard()

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
            boxScores = newBoxScores.flatMap { it.teams }
        }
    }
}

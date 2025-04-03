package com.example.plugins

import com.example.messageTypes.sports.AllStandings
import com.example.messageTypes.sports.FullTeam
import com.example.messageTypes.sports.Scoreboard
import com.example.messageTypes.sports.Team
import com.example.testRestClient.sport.SportOperationAdapter
import io.klogging.Klogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SportHandler(private val sportOperationAdapter: SportOperationAdapter) : Klogging {
    var scoreboard: Scoreboard? = null
    var teams: List<Team> = emptyList()
    var standings: AllStandings? = null
    var boxScores: List<FullTeam> = listOf()

    private val boxScoreFlowMut = MutableSharedFlow<List<FullTeam>>(replay = 0)
    val boxScoreFlow = boxScoreFlowMut.asSharedFlow()

    suspend fun getStandings() {
        standings = sportOperationAdapter.getStandings()
    }

    suspend fun getTeams() {
        teams = sportOperationAdapter.getTeams()
    }
    suspend fun buildScoreboard() {
        coroutineScope {
            scoreboard = sportOperationAdapter.getScoreboard()
            scoreboard?.competitions?.map { it.id }?.let {gameIds ->
                fetchAndMapGames(gameIds)
            }
            while (isActive) {
                delay(10 * 1000)
                scoreboard?.let { sb ->
                    sb.competitions.map { Triple(it.status.state, it.date, it.id) }.let { games ->
                        val earliestTime = games.minOf{ Instant.parse(it.second) }
                        val allStates = games.map { it.first }.toSet()
                        if (Clock.System.now() > earliestTime) {
                            scoreboard = sportOperationAdapter.getScoreboard()

                            //If boxscores already filled once then filter out games not in progress
//                            DISABLE BOX SCORE UPDATES FOR NOW
//                            games.filter { it.second == "in" }.map { it.third }.let {gameIds ->
//                                fetchAndMapGames(gameIds)
//                            }
                            if(!allStates.contains("pre") && !allStates.contains("in")){
                                delay(60 * 60 * 1000)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchAndMapGames(gameIds: List<String>) = coroutineScope {
        gameIds.map { gameId ->
            async{
                sportOperationAdapter.getGameStats(gameId)
            }
        }.awaitAll().filterNotNull().also { newBoxScores ->
            //map result to teams
            boxScores = newBoxScores.flatMap { it.teams }
        }
    }
}

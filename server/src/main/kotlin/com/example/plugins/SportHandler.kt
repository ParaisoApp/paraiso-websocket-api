package com.example.plugins

import com.example.messageTypes.sports.AllStandings
import com.example.messageTypes.sports.FullTeam
import com.example.messageTypes.sports.Scoreboard
import com.example.messageTypes.sports.Team
import com.example.testRestClient.sport.SportOperationAdapter
import io.klogging.Klogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
            launch { updateScores() }
            while (isActive) {
                // Delay for 1 minute (60 seconds * 1000 milliseconds)
                delay(10 * 1000)
                scoreboard?.let { sb ->
                    sb.competitions.map { it.status.state }.toSet().let { allStates ->
                        if (allStates.contains("in")) {
                            scoreboard = sportOperationAdapter.getScoreboard()
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateScores() {
        coroutineScope {
            while (isActive) {
                scoreboard?.competitions?.mapNotNull {
                    sportOperationAdapter.getGameStats(it.id)
                }?.also { newBoxScores ->
                    val boxScoreMappedToTeam = newBoxScores.flatMap { it.teams }
                    boxScoreFlowMut.emit(boxScoreMappedToTeam)
                    boxScores = boxScoreMappedToTeam
                }
                delay(30000000L)
            }
        }
    }
}

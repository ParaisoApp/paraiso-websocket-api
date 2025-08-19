package com.paraiso.com.paraiso.server.plugins.jobs.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.data.FullTeam
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.ScoreboardResponse
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.domain.sport.sports.bball.BBallState
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BBallJobs(private val bBallApi: BBallApi) {

    suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: ScoreboardResponse? = null
                while (isActive) {
                    val scoreboard = bBallApi.getScoreboard()
                    if (scoreboard != null && lastSentScoreboard != scoreboard) {
                        session.sendTypedMessage(MessageType.SCOREBOARD, scoreboard)
                        lastSentScoreboard = scoreboard
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    val boxScores = BBallState.boxScores
                    if (boxScores.isNotEmpty() && lastSentBoxScores != boxScores) {
                        session.sendTypedMessage(MessageType.BOX_SCORES, boxScores.map { it.toResponse() })
                        lastSentBoxScores = boxScores
                    }
                    delay(5 * 1000)
                }
            }
        )
    }
    suspend fun teamJobs(content: String?, session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: ScoreboardResponse? = null
                while (isActive) {
                    val scoreboard = bBallApi.getScoreboard()
                    scoreboard?.let { sb ->
                        val filteredSb = scoreboard.copy(
                            competitions = sb.competitions.filter { comp -> comp.teams.map { it.teamId }.contains(content) }
                        )
                        if (lastSentScoreboard != filteredSb) {
                            session.sendTypedMessage(MessageType.SCOREBOARD, filteredSb)
                            lastSentScoreboard = filteredSb
                        }
                        delay(5 * 1000)
                    } ?: run { delay(5 * 1000L) }
                }
            },
            launch {
                while (isActive) {
                    val currentBoxScores = BBallState.boxScores
                    val scoreboard = bBallApi.getScoreboard()

                    if (currentBoxScores.isNotEmpty() && scoreboard != null) {
                        scoreboard.competitions.firstOrNull { comp ->
                            comp.teams.map { it.teamId }.contains(content)
                        }?.teams?.map { it.teamId }
                            ?.let { teamIds ->
                                currentBoxScores.filter { boxScore -> teamIds.contains(boxScore.teamId) }
                                    .map { it.toResponse() }
                                    .let { filteredBoxScores ->
                                        session.sendTypedMessage(MessageType.BOX_SCORES, filteredBoxScores)
                                    }
                            }
                        // delay(5 * 1000)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000L)
                    }
                }
            }
        )
    }
}

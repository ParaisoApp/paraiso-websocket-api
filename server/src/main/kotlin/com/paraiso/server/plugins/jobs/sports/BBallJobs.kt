package com.paraiso.com.paraiso.server.plugins.jobs.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.data.FullTeam
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.bball.BBallState
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BBallJobs {

    suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    val scoreboard = BBallState.scoreboard
                    if (scoreboard != null && lastSentScoreboard != scoreboard) {
                        session.sendTypedMessage(MessageType.SCOREBOARD, scoreboard.toResponse())
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
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    val currentScoreboard = BBallState.scoreboard
                    currentScoreboard?.let { sb ->
                        val filteredSb = currentScoreboard.copy(
                            competitions = sb.competitions.filter { comp -> comp.teams.map { it.team.id }.contains(content) }
                        )
                        if (lastSentScoreboard != filteredSb) {
                            session.sendTypedMessage(MessageType.SCOREBOARD, filteredSb.toResponse())
                            lastSentScoreboard = filteredSb
                        }
                        delay(5 * 1000)
                    } ?: run { delay(5 * 1000L) }
                }
            },
            launch {
                while (isActive) {
                    val currentBoxScores = BBallState.boxScores
                    val currentScoreboard = BBallState.scoreboard

                    if (currentBoxScores.isNotEmpty() && currentScoreboard != null) {
                        currentScoreboard.competitions.firstOrNull { comp ->
                            comp.teams.map { it.team.id }.contains(content)
                        }?.teams?.map { it.team.id }
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

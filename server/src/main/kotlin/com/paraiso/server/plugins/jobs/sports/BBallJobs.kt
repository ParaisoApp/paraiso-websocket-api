package com.paraiso.com.paraiso.server.plugins.jobs.sports

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.data.FullTeam
import com.paraiso.domain.sport.data.ScoreboardResponse
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BBallJobs(private val bBallApi: BBallApi) {
    private var lastSentScoreboard: ScoreboardResponse? = null
    suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                lastSentScoreboard = null
                while (isActive) {
                    bBallApi.getScoreboard()?.let { sb ->
                        if (lastSentScoreboard != sb) {
                            session.sendTypedMessage(MessageType.SCOREBOARD, sb)
                            lastSentScoreboard = sb
                        }
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    lastSentScoreboard?.competitions?.map { it.id }?.let{ compIds ->
                        val boxScores = bBallApi.getBoxscores(compIds).flatMap { it.teams }
                        if (boxScores.isNotEmpty() && lastSentBoxScores != boxScores) {
                            session.sendTypedMessage(MessageType.BOX_SCORES, boxScores.map { it.toResponse() })
                            lastSentBoxScores = boxScores
                        }
                    }
                    delay(5 * 1000)
                }
            }
        )
    }
    suspend fun teamJobs(content: String?, session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                lastSentScoreboard = null
                while (isActive) {
                    bBallApi.getScoreboard()?.let { sb ->
                        val filteredSb = sb.copy(
                            competitions = sb.competitions.filter { comp -> comp.teams.map { it.teamId }.contains(content) }
                        )
                        if (lastSentScoreboard != filteredSb) {
                            session.sendTypedMessage(MessageType.SCOREBOARD, filteredSb)
                            lastSentScoreboard = filteredSb
                        }
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    lastSentScoreboard?.competitions?.firstOrNull()?.let { competition ->
                        val boxScores = bBallApi.getBoxscores(listOf(competition.id)).flatMap { it.teams }
                        if (boxScores.isNotEmpty() && lastSentBoxScores != boxScores) {
                            session.sendTypedMessage(MessageType.BOX_SCORES, boxScores.map { it.toResponse() })
                            lastSentBoxScores = boxScores
                            delay(6 * 60 * 60 * 1000)
                        } else {
                            delay(5 * 1000L)
                        }
                    }
                }
            }
        )
    }
}

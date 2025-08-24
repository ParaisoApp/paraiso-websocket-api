package com.paraiso.com.paraiso.server.plugins.jobs

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.data.toResponse
import com.paraiso.domain.sport.sports.SportState
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SportJobs {
    suspend fun sportJobs(
        session: WebSocketServerSession,
        sport: String
    ) = coroutineScope {
        listOf(
            launch {
                SportState.getScoreboardFlow(sport).collect { sb ->
                    session.sendTypedMessage(MessageType.SCOREBOARD, sb.toResponse())
                }
            },
            launch {
                SportState.getBoxscoreFlow(sport).collect { boxscores ->
                    session.sendTypedMessage(
                        MessageType.BOX_SCORES,
                        boxscores.flatMap { it.teams }.map { it.toResponse() }
                    )
                }
            }
        )
    }
    suspend fun teamJobs(
        content: String?,
        session: WebSocketServerSession,
        sport: String
    ) = coroutineScope {
        listOf(
            launch {
                SportState.getScoreboardFlow(sport).collect { sb ->
                    val sbResponse = sb.copy(
                        competitions = sb.competitions
                            .filter { comp -> comp.teams.map { it.teamId }.contains(content) }
                    ).toResponse()
                    session.sendTypedMessage(MessageType.SCOREBOARD, sbResponse)
                }
            },
            launch {
                SportState.getBoxscoreFlow(sport).collect { boxscores ->
                    val boxScoresResponse = boxscores
                        .filter { boxScore ->
                            boxScore.teams.map { it.teamId }.contains(content)
                        }.map { it.toResponse() }
                    session.sendTypedMessage(MessageType.BOX_SCORES, boxScoresResponse.flatMap { it.teams })
                }
            }
        )
    }
}

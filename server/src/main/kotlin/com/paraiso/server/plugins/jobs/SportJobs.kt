package com.paraiso.com.paraiso.server.plugins.jobs

import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.sport.SportState
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SportJobs {

    suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    if (SportState.scoreboard != null && lastSentScoreboard != SportState.scoreboard) {
                        session.sendTypedMessage(MessageType.SCOREBOARD, SportState.scoreboard)
                        lastSentScoreboard = SportState.scoreboard
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    SportState.standings?.let {
                        session.sendTypedMessage(MessageType.STANDINGS, it)
                        delay(6 * 60 * 60 * 1000)
                    } ?: run {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    if (SportState.boxScores.isNotEmpty() && lastSentBoxScores != SportState.boxScores) {
                        session.sendTypedMessage(MessageType.BOX_SCORES, SportState.boxScores)
                        lastSentBoxScores = SportState.boxScores
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (SportState.rosters.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.ROSTERS, SportState.rosters)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    SportState.leaders?.let {
                        session.sendTypedMessage(MessageType.LEADERS, it)
                        delay(6 * 60 * 60 * 1000)
                    } ?: run {
                        delay(5 * 1000)
                    }
                }
            }
        )
    }
    suspend fun teamJobs(content: String, session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    val currentScoreboard = SportState.scoreboard
                    currentScoreboard?.let { sb ->
                        val filteredSb = currentScoreboard.copy(
                            competitions = sb.competitions.filter { comp -> comp.teams.map { it.team.id }.contains(content) }
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
                    val currentBoxScores = SportState.boxScores
                    val currentScoreboard = SportState.scoreboard

                    if (currentBoxScores.isNotEmpty() && currentScoreboard != null) {
                        currentScoreboard.competitions.firstOrNull { comp ->
                            comp.teams.map { it.team.id }.contains(content)
                        }?.teams?.map { it.team.id }
                            ?.let { teamIds ->
                                currentBoxScores.filter { boxScore -> teamIds.contains(boxScore.teamId) }
                                    .let { filteredBoxScores ->
                                        session.sendTypedMessage(MessageType.BOX_SCORES, filteredBoxScores)
                                    }
                            }
                        //delay(5 * 1000)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000L)
                    }
                }
            },
            launch {
                while (isActive) {
                    if (SportState.rosters.isNotEmpty()) {
                        val filterRosters = SportState.rosters.find { it.team.id == content }
                        session.sendTypedMessage(MessageType.ROSTERS, listOf(filterRosters))
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    if (SportState.schedules.isNotEmpty()) {
                        val filterRosters = SportState.schedules.find { it.team.id == content }
                        session.sendTypedMessage(MessageType.SCHEDULE, filterRosters)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
        )
    }
}
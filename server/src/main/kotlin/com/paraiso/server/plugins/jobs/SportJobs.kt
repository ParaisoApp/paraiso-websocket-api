package com.paraiso.com.paraiso.server.plugins.jobs

import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.server.util.sendTypedMessage
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SportJobs(private val sportHandler: SportHandler) {

    suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    if (sportHandler.scoreboard != null && lastSentScoreboard != sportHandler.scoreboard) {
                        session.sendTypedMessage(MessageType.SCOREBOARD, sportHandler.scoreboard)
                        lastSentScoreboard = sportHandler.scoreboard
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.TEAMS, sportHandler.teams)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.standings?.let {
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
                    if (sportHandler.boxScores.isNotEmpty() && lastSentBoxScores != sportHandler.boxScores) {
                        session.sendTypedMessage(MessageType.BOX_SCORES, sportHandler.boxScores)
                        lastSentBoxScores = sportHandler.boxScores
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.rosters.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.ROSTERS, sportHandler.rosters)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.leaders?.let {
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
                    val currentScoreboard = sportHandler.scoreboard
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
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(MessageType.TEAMS, sportHandler.teams)
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000L)
                    }
                }
            },
            launch {
                while (isActive) {
                    val currentBoxScores = sportHandler.boxScores
                    val currentScoreboard = sportHandler.scoreboard

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
                    if (sportHandler.rosters.isNotEmpty()) {
                        val filterRosters = sportHandler.rosters.find { it.team.id == content }
                        session.sendTypedMessage(MessageType.ROSTERS, listOf(filterRosters))
                        delay(6 * 60 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.schedules.isNotEmpty()) {
                        val filterRosters = sportHandler.schedules.find { it.team.id == content }
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
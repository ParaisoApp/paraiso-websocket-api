package com.paraiso.server.plugins

import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.server.messageTypes.MessageType
import com.paraiso.server.messageTypes.Vote
import com.paraiso.server.util.ServerConfig
import com.paraiso.server.util.findCorrectConversion
import com.paraiso.server.util.sendTypedMessage
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.websocket.close
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler(private val sportHandler: SportHandler) : Klogging {
    companion object {
        private val serverConfig = ServerConfig()
    }

    private val userList: MutableMap<String, _root_ide_package_.com.paraiso.server.messageTypes.User> = mutableMapOf()
    private val banList: MutableSet<String> = mutableSetOf()

    private val messageFlowMut = MutableSharedFlow<com.paraiso.server.messageTypes.Message>(replay = 0)
    private val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    private val deleteFlowMut = MutableSharedFlow<com.paraiso.server.messageTypes.Delete>(replay = 0)
    private val basicFlowMut = MutableSharedFlow<String>(replay = 0)
    private val userLoginFlowMut = MutableSharedFlow<_root_ide_package_.com.paraiso.server.messageTypes.User>(replay = 0)
    private val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)
    private val banUserFlowMut = MutableSharedFlow<com.paraiso.server.messageTypes.Ban>(replay = 0)

    private val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteFlowMut.asSharedFlow()),
        Pair(MessageType.BASIC, basicFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LOGIN, userLoginFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LEAVE, userLeaveFlowMut.asSharedFlow()),
        Pair(MessageType.BAN, banUserFlowMut.asSharedFlow())
    )

    suspend fun handleUser(session: WebSocketServerSession) {
        UUID.randomUUID().toString().let { id ->
            val currentUser = _root_ide_package_.com.paraiso.server.messageTypes.User(
                id = id,
                name = com.paraiso.server.messageTypes.randomGuestName(),
                banned = false,
                roles = _root_ide_package_.com.paraiso.server.messageTypes.UserRole.GUEST,
                websocket = session
            )
            userList[id] = currentUser
            session.joinChat(currentUser)
        }
    }

    private suspend fun handleRoute(route: com.paraiso.server.messageTypes.Route, session: WebSocketServerSession): List<Job> = coroutineScope {
        when (route.route) {
            com.paraiso.server.messageTypes.SiteRoute.HOME -> homeJobs(session)
            com.paraiso.server.messageTypes.SiteRoute.PROFILE -> profileJobs(route.content, session)
            com.paraiso.server.messageTypes.SiteRoute.SPORT -> sportJobs(session)
            com.paraiso.server.messageTypes.SiteRoute.TEAM -> teamJobs(route.content, session)
        }
    }

    private suspend fun homeJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(launch {})
    }
    private suspend fun profileJobs(content: String, session: WebSocketServerSession) = coroutineScope {
        listOf(launch {})
    }
    private suspend fun teamJobs(content: String, session: WebSocketServerSession) = coroutineScope {
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
                            session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.SCOREBOARD, filteredSb)
                            lastSentScoreboard = filteredSb
                        }
                        delay(5 * 1000)
                    } ?: run { delay(5 * 1000L) }
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.TEAMS, sportHandler.teams)
                        delay(24 * 60 * 1000)
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
                                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.BOX_SCORES, filteredBoxScores)
                                    }
                            }
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000L)
                    }
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.rosters.isNotEmpty()) {
                        val filterRosters = sportHandler.rosters.filter { it.team.id == content }
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.ROSTERS, filterRosters)
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            }
        )
    }

    private suspend fun sportJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(
            launch {
                var lastSentScoreboard: Scoreboard? = null
                while (isActive) {
                    if (sportHandler.scoreboard != null && lastSentScoreboard != sportHandler.scoreboard) {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.SCOREBOARD, sportHandler.scoreboard)
                        lastSentScoreboard = sportHandler.scoreboard
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.teams.isNotEmpty()) {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.TEAMS, sportHandler.teams)
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.standings?.let {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.STANDINGS, it)
                        delay(24 * 60 * 1000)
                    } ?: run {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                var lastSentBoxScores = listOf<FullTeam>()
                while (isActive) {
                    if (sportHandler.boxScores.isNotEmpty() && lastSentBoxScores != sportHandler.boxScores) {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.BOX_SCORES, sportHandler.boxScores)
                        lastSentBoxScores = sportHandler.boxScores
                    }
                    delay(5 * 1000)
                }
            },
            launch {
                while (isActive) {
                    if (sportHandler.rosters.isNotEmpty()) {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.ROSTERS, sportHandler.rosters)
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.leaders?.let {
                        session.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.LEADERS, it)
                        delay(24 * 60 * 1000)
                    } ?: run {
                        delay(5 * 1000)
                    }
                }
            }
        )
    }

    private suspend fun WebSocketServerSession.joinChat(user: _root_ide_package_.com.paraiso.server.messageTypes.User) {
        var sessionUser = user.copy()
        sendTypedMessage(com.paraiso.server.messageTypes.MessageType.USER, sessionUser.copy(websocket = null))
        sendTypedMessage(com.paraiso.server.messageTypes.MessageType.USER_LIST, userList.values.map { it.copy(websocket = null) })

        val messageCollectionJobs = flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        com.paraiso.server.messageTypes.MessageType.MSG -> sendTypedMessage(type, message as com.paraiso.server.messageTypes.Message)
                        com.paraiso.server.messageTypes.MessageType.VOTE -> sendTypedMessage(type, message as Vote)
                        com.paraiso.server.messageTypes.MessageType.DELETE -> sendTypedMessage(type, message as com.paraiso.server.messageTypes.Delete)
                        com.paraiso.server.messageTypes.MessageType.BASIC -> sendTypedMessage(type, message as String)
                        com.paraiso.server.messageTypes.MessageType.USER_LOGIN -> sendTypedMessage(type, message as _root_ide_package_.com.paraiso.server.messageTypes.User)
                        com.paraiso.server.messageTypes.MessageType.USER_LEAVE -> sendTypedMessage(type, message as String)
                        com.paraiso.server.messageTypes.MessageType.BAN -> {
                            val ban = message as? com.paraiso.server.messageTypes.Ban
                            ban?.let { bannedMsg ->
                                if (sessionUser.id == bannedMsg.userId) {
                                    sessionUser = sessionUser.copy(banned = true)
                                }
                            }
                        }
                        else -> logger.error { "Found unknown type when sending typed message from flow $sharedFlow" }
                    }
                }
            }
        }

        userLoginFlowMut.emit(sessionUser.copy(websocket = null))
        // holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageWithType = converter?.findCorrectConversion<com.paraiso.server.messageTypes.TypeMapping<String>>(frame)
                    ?.typeMapping?.entries?.first()
                when (messageWithType?.key) {
                    com.paraiso.server.messageTypes.MessageType.MSG -> {
                        Json.decodeFromString<com.paraiso.server.messageTypes.Message>(messageWithType.value).let { message ->
                            message.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { messageWithData ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(com.paraiso.server.messageTypes.MessageType.MSG, messageWithData)
                                } else {
                                    messageFlowMut.emit(messageWithData)
                                }
                            }
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.DM -> {
                        Json.decodeFromString<com.paraiso.server.messageTypes.DirectMessage>(messageWithType.value).let { dm ->
                            dm.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { dmWithData ->
                                launch { sendTypedMessage(com.paraiso.server.messageTypes.MessageType.DM, dmWithData) }
                                if (!sessionUser.banned) {
                                    userList[dmWithData.userReceiveId]
                                        ?.websocket?.sendTypedMessage(com.paraiso.server.messageTypes.MessageType.DM, dmWithData)
                                }
                            }
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.VOTE -> {
                        val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(com.paraiso.server.messageTypes.MessageType.VOTE, vote)
                        } else {
                            voteFlowMut.emit(vote)
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.DELETE -> {
                        val delete = Json.decodeFromString<com.paraiso.server.messageTypes.Delete>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(com.paraiso.server.messageTypes.MessageType.DELETE, delete)
                        } else {
                            deleteFlowMut.emit(delete)
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.LOGIN -> {
                        val login = Json.decodeFromString<com.paraiso.server.messageTypes.Login>(messageWithType.value)
                        if (login.password == serverConfig.admin) {
                            sessionUser.copy(
                                roles = _root_ide_package_.com.paraiso.server.messageTypes.UserRole.ADMIN,
                                name = "Breeze"
                            ).let { admin ->
                                sessionUser = admin
                                userList[sessionUser.id] = admin
                                admin.copy(websocket = null).let { adminRef ->
                                    sendTypedMessage(com.paraiso.server.messageTypes.MessageType.USER, adminRef)
                                    userLoginFlowMut.emit(adminRef)
                                }
                            }
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.BAN -> {
                        val ban = Json.decodeFromString<com.paraiso.server.messageTypes.Ban>(messageWithType.value)
                        if (sessionUser.roles == _root_ide_package_.com.paraiso.server.messageTypes.UserRole.ADMIN) {
                            banUserFlowMut.emit(ban)
                            banList.add(ban.userId)
                        }
                    }
                    com.paraiso.server.messageTypes.MessageType.ROUTE -> {
                        val route = Json.decodeFromString<com.paraiso.server.messageTypes.Route>(messageWithType.value)
                        activeJobs?.cancelAndJoin()
                        val session = this
                        activeJobs = launch {
                            handleRoute(route, session)
                        }
                    }
                    else -> logger.error { "Invalid message type received $messageWithType" }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Error parsing incoming data" }
        } finally {
            messageCollectionJobs.forEach { it.cancelAndJoin() }
            activeJobs?.cancelAndJoin()
            userLeaveFlowMut.emit(sessionUser.id)
            userList.remove(sessionUser.id)
            sessionUser.websocket?.close()
        }
    }
}

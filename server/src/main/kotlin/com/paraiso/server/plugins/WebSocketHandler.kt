package com.paraiso.server.plugins

import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.server.messageTypes.Ban
import com.paraiso.server.messageTypes.Delete
import com.paraiso.server.messageTypes.DirectMessage
import com.paraiso.server.messageTypes.Login
import com.paraiso.server.messageTypes.Message
import com.paraiso.server.messageTypes.MessageType
import com.paraiso.server.messageTypes.Route
import com.paraiso.server.messageTypes.SiteRoute
import com.paraiso.server.messageTypes.TypeMapping
import com.paraiso.server.messageTypes.User
import com.paraiso.server.messageTypes.UserRole
import com.paraiso.server.messageTypes.UserStatus
import com.paraiso.server.messageTypes.Vote
import com.paraiso.server.messageTypes.randomGuestName
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

    private val userList: MutableMap<String, User> = mutableMapOf()
    private val banList: MutableSet<String> = mutableSetOf()

    private val messageFlowMut = MutableSharedFlow<Message>(replay = 0)
    private val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    private val deleteFlowMut = MutableSharedFlow<Delete>(replay = 0)
    private val basicFlowMut = MutableSharedFlow<String>(replay = 0)
    private val userLoginFlowMut = MutableSharedFlow<User>(replay = 0)
    private val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)
    private val banUserFlowMut = MutableSharedFlow<Ban>(replay = 0)

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
        userList[session.call.request.cookies["guest_id"] ?: ""]?.let { currentUser ->
            session.joinChat(currentUser.copy(
                status = UserStatus.DISCONNECTED,
                websocket = session
            ))
        } ?: run {
            UUID.randomUUID().toString().let { id ->
                val currentUser = User(
                    id = id,
                    name = randomGuestName(),
                    banned = false,
                    roles = UserRole.GUEST,
                    websocket = session,
                    status = UserStatus.CONNECTED,
                    lastSeen = System.currentTimeMillis()
                )
                userList[id] = currentUser
                session.joinChat(currentUser)
            }
        }
    }

    private suspend fun handleRoute(route: Route, session: WebSocketServerSession): List<Job> = coroutineScope {
        when (route.route) {
            SiteRoute.HOME -> homeJobs(session)
            SiteRoute.PROFILE -> profileJobs(route.content, session)
            SiteRoute.SPORT -> sportJobs(session)
            SiteRoute.TEAM -> teamJobs(route.content, session)
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
                                        session.sendTypedMessage(MessageType.BOX_SCORES, filteredBoxScores)
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
                        session.sendTypedMessage(MessageType.ROSTERS, filterRosters)
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
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.standings?.let {
                        session.sendTypedMessage(MessageType.STANDINGS, it)
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
                        delay(24 * 60 * 1000)
                    } else {
                        delay(5 * 1000)
                    }
                }
            },
            launch {
                while (isActive) {
                    sportHandler.leaders?.let {
                        session.sendTypedMessage(MessageType.LEADERS, it)
                        delay(24 * 60 * 1000)
                    } ?: run {
                        delay(5 * 1000)
                    }
                }
            }
        )
    }

    private suspend fun WebSocketServerSession.joinChat(user: User) {
        var sessionUser = user.copy()
        sendTypedMessage(MessageType.USER, sessionUser.copy(websocket = null))
        sendTypedMessage(MessageType.USER_LIST, userList.values.map { it.copy(websocket = null) })

        val messageCollectionJobs = flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> sendTypedMessage(type, message as Message)
                        MessageType.VOTE -> sendTypedMessage(type, message as Vote)
                        MessageType.DELETE -> sendTypedMessage(type, message as Delete)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_LOGIN -> sendTypedMessage(type, message as User)
                        MessageType.USER_LEAVE -> sendTypedMessage(type, message as String)
                        MessageType.BAN -> {
                            val ban = message as? Ban
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
                val messageWithType = converter?.findCorrectConversion<TypeMapping<String>>(frame)
                    ?.typeMapping?.entries?.first()
                when (messageWithType?.key) {
                    MessageType.MSG -> {
                        Json.decodeFromString<Message>(messageWithType.value).let { message ->
                            message.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { messageWithData ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.MSG, messageWithData)
                                } else {
                                    messageFlowMut.emit(messageWithData)
                                }
                            }
                        }
                    }
                    MessageType.DM -> {
                        Json.decodeFromString<DirectMessage>(messageWithType.value).let { dm ->
                            dm.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { dmWithData ->
                                launch { sendTypedMessage(MessageType.DM, dmWithData) }
                                if (!sessionUser.banned) {
                                    userList[dmWithData.userReceiveId]
                                        ?.websocket?.sendTypedMessage(MessageType.DM, dmWithData)
                                }
                            }
                        }
                    }
                    MessageType.VOTE -> {
                        val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(MessageType.VOTE, vote)
                        } else {
                            voteFlowMut.emit(vote)
                        }
                    }
                    MessageType.DELETE -> {
                        val delete = Json.decodeFromString<Delete>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(MessageType.DELETE, delete)
                        } else {
                            deleteFlowMut.emit(delete)
                        }
                    }
                    MessageType.LOGIN -> {
                        val login = Json.decodeFromString<Login>(messageWithType.value)
                        if (login.password == serverConfig.admin) {
                            sessionUser.copy(
                                roles = UserRole.ADMIN,
                                name = "Breeze"
                            ).let { admin ->
                                sessionUser = admin
                                userList[sessionUser.id] = admin
                                admin.copy(websocket = null).let { adminRef ->
                                    sendTypedMessage(MessageType.USER, adminRef)
                                    userLoginFlowMut.emit(adminRef)
                                }
                            }
                        }
                    }
                    MessageType.BAN -> {
                        val ban = Json.decodeFromString<Ban>(messageWithType.value)
                        if (sessionUser.roles == UserRole.ADMIN) {
                            banUserFlowMut.emit(ban)
                            banList.add(ban.userId)
                        }
                    }
                    MessageType.ROUTE -> {
                        val route = Json.decodeFromString<Route>(messageWithType.value)
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
            userList[sessionUser.id]?.let{ disconnectUser ->
                userList[sessionUser.id] = disconnectUser.copy(
                    status = UserStatus.DISCONNECTED,
                    websocket = null
                )
            }
            sessionUser.websocket?.close()
        }
    }
}

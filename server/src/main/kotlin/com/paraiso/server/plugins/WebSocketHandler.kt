package com.paraiso.server.plugins

import com.paraiso.domain.auth.UserRole
import com.paraiso.domain.auth.UserStatus
import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.util.ServerState
import com.paraiso.domain.util.messageTypes.MessageType
import com.paraiso.domain.util.messageTypes.SiteRoute
import com.paraiso.domain.util.messageTypes.randomGuestName
import com.paraiso.server.messageTypes.User
import com.paraiso.server.messageTypes.toDomain
import com.paraiso.server.messageTypes.toResponse
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import com.paraiso.domain.auth.User as UserDomain
import com.paraiso.domain.util.messageTypes.Ban as BanDomain
import com.paraiso.domain.util.messageTypes.Delete as DeleteDomain
import com.paraiso.domain.util.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.util.messageTypes.Login as LoginDomain
import com.paraiso.domain.util.messageTypes.Message as MessageDomain
import com.paraiso.domain.util.messageTypes.Route as RouteDomain
import com.paraiso.domain.util.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.util.messageTypes.Vote as VoteDomain

class WebSocketHandler(private val sportHandler: SportHandler) : Klogging {
    companion object {
        private val serverConfig = ServerConfig()
    }
    private val userToSocket: MutableMap<String, WebSocketServerSession> = mutableMapOf()

    suspend fun handleUser(session: WebSocketServerSession) {
        ServerState.userList[session.call.request.cookies["guest_id"] ?: ""]?.let { currentUser ->
            session.joinChat(
                currentUser.toResponse().copy(
                    status = UserStatus.DISCONNECTED
                )
            )
        } ?: run {
            UUID.randomUUID().toString().let { id ->
                val currentUser = User(
                    id = id,
                    name = randomGuestName(),
                    banned = false,
                    roles = UserRole.GUEST,
                    status = UserStatus.CONNECTED,
                    lastSeen = System.currentTimeMillis()
                )
                ServerState.userList[id] = currentUser.toDomain()
                userToSocket[id] = session
                session.joinChat(currentUser)
            }
        }
    }

    private suspend fun handleRoute(route: RouteDomain, session: WebSocketServerSession): List<Job> = coroutineScope {
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
        sendTypedMessage(MessageType.USER, sessionUser)
        sendTypedMessage(MessageType.USER_LIST, ServerState.userList.values.map { it.toResponse() })

        val messageCollectionJobs = ServerState.flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> sendTypedMessage(type, message as MessageDomain)
                        MessageType.VOTE -> sendTypedMessage(type, message as VoteDomain)
                        MessageType.DELETE -> sendTypedMessage(type, message as DeleteDomain)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_LOGIN -> {
                            val userLogin = message as? UserDomain
                            if (sessionUser.id == userLogin?.id) {
                                sendTypedMessage(MessageType.USER, userLogin)
                            }else{
                                sendTypedMessage(type, userLogin)
                            }
                        }
                        MessageType.USER_LEAVE -> sendTypedMessage(type, message as String)
                        MessageType.BAN -> {
                            val ban = message as? BanDomain
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

        ServerState.userLoginFlowMut.emit(sessionUser.toDomain())
        // holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageWithType = converter?.findCorrectConversion<TypeMappingDomain<String>>(frame)
                    ?.typeMapping?.entries?.first()
                when (messageWithType?.key) {
                    MessageType.MSG -> {
                        Json.decodeFromString<MessageDomain>(messageWithType.value).let { message ->
                            message.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { messageWithData ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.MSG, messageWithData)
                                } else {
                                    ServerState.messageFlowMut.emit(messageWithData)
                                }
                            }
                        }
                    }
                    MessageType.DM -> {
                        Json.decodeFromString<DirectMessageDomain>(messageWithType.value).let { dm ->
                            dm.copy(
                                id = UUID.randomUUID().toString(),
                                userId = sessionUser.id
                            ).let { dmWithData ->
                                launch { sendTypedMessage(MessageType.DM, dmWithData) }
                                if (!sessionUser.banned) {
                                    userToSocket[dmWithData.userReceiveId]?.sendTypedMessage(MessageType.DM, dmWithData)
                                }
                            }
                        }
                    }
                    MessageType.VOTE -> {
                        val vote = Json.decodeFromString<VoteDomain>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(MessageType.VOTE, vote)
                        } else {
                            ServerState.voteFlowMut.emit(vote)
                        }
                    }
                    MessageType.DELETE -> {
                        val delete = Json.decodeFromString<DeleteDomain>(messageWithType.value).copy(userId = sessionUser.id)
                        if (sessionUser.banned) {
                            sendTypedMessage(MessageType.DELETE, delete)
                        } else {
                            ServerState.deleteFlowMut.emit(delete)
                        }
                    }
//                    MessageType.LOGIN -> {
//                        val login = Json.decodeFromString<LoginDomain>(messageWithType.value)
//                        if (login.password == serverConfig.admin) {
//                            sessionUser.copy(
//                                roles = UserRole.ADMIN,
//                                name = "Breeze"
//                            ).let { admin ->
//                                sessionUser = admin
//                                ServerState.userList[sessionUser.id] = admin.toDomain()
//                                sendTypedMessage(MessageType.USER, admin)
//                                ServerState.userLoginFlowMut.emit(admin.toDomain())
//                            }
//                        }
//                    }
                    MessageType.BAN -> {
                        val ban = Json.decodeFromString<BanDomain>(messageWithType.value)
                        if (sessionUser.roles == UserRole.ADMIN) {
                            ServerState.banUserFlowMut.emit(ban)
                            ServerState.banList.add(ban.userId)
                        }
                    }
                    MessageType.ROUTE -> {
                        val route = Json.decodeFromString<RouteDomain>(messageWithType.value)
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
            ServerState.userLeaveFlowMut.emit(sessionUser.id)
            ServerState.userList[sessionUser.id]?.let { disconnectUser ->
                ServerState.userList[sessionUser.id] = disconnectUser.copy(
                    status = UserStatus.DISCONNECTED
                )
            }
            userToSocket[sessionUser.id]?.close()
        }
    }
}

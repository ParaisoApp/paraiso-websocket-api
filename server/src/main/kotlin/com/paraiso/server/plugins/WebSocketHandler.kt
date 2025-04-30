package com.paraiso.server.plugins

import com.paraiso.com.paraiso.api.auth.User
import com.paraiso.com.paraiso.api.auth.toDomain
import com.paraiso.com.paraiso.api.auth.toResponse
import com.paraiso.com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.com.paraiso.server.plugins.jobs.SportJobs
import com.paraiso.com.paraiso.server.util.SessionState
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.SiteRoute
import com.paraiso.domain.messageTypes.randomGuestName
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.users.initSettings
import com.paraiso.domain.util.Constants.EMPTY
import com.paraiso.domain.util.ServerState
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.util.UUID
import com.paraiso.domain.messageTypes.Ban as BanDomain
import com.paraiso.domain.messageTypes.Block as BlockDomain
import com.paraiso.domain.messageTypes.Delete as DeleteDomain
import com.paraiso.domain.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.messageTypes.Message as MessageDomain
import com.paraiso.domain.messageTypes.Route as RouteDomain
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.messageTypes.Vote as VoteDomain
import com.paraiso.domain.messageTypes.FilterTypes as FilterTypesDomain
import com.paraiso.domain.users.User as UserDomain

class WebSocketHandler(usersApi: UsersApi, postsApi: PostsApi) : Klogging {
    //jobs
    private val homeJobs = HomeJobs()
    private val profileJobs = ProfileJobs()
    private val sportJobs = SportJobs()
    //users
    private val userToSocket: MutableMap<String, WebSocketServerSession> = mutableMapOf()
    private val usersApiRef = usersApi
    //posts
    private val postsApiRef = postsApi
    //session state
    private val sessionState = SessionState()

    suspend fun handleUser(session: WebSocketServerSession) {
        ServerState.userList[session.call.request.cookies["guest_id"] ?: ""]?.let { currentUser ->
            session.joinChat(
                currentUser.toResponse().copy(
                    status = UserStatus.CONNECTED
                )
            )
        } ?: run {
            UUID.randomUUID().toString().let { id ->
                val currentUser = User(
                    id = id,
                    name = randomGuestName(),
                    posts = emptyMap(),
                    comments = emptyMap(),
                    replies = emptyMap(),
                    roles = UserRole.GUEST,
                    banned = false,
                    status = UserStatus.CONNECTED,
                    blockList = emptySet(),
                    image = EMPTY,
                    lastSeen = System.currentTimeMillis(),
                    settings = UserSettings.initSettings(),
                    createdOn = Clock.System.now(),
                    updatedOn = Clock.System.now()
                )
                ServerState.userList[id] = currentUser.toDomain()
                userToSocket[id] = session
                session.joinChat(currentUser)
            }
        }
    }

    private suspend fun handleRoute(route: RouteDomain, session: WebSocketServerSession): List<Job> = coroutineScope {
        when (route.route) {
            SiteRoute.HOME -> homeJobs.homeJobs(session)
            SiteRoute.PROFILE -> profileJobs.profileJobs(route.content, session)
            SiteRoute.SPORT -> sportJobs.sportJobs(session)
            SiteRoute.TEAM -> sportJobs.teamJobs(route.content, session)
        }
    }

    private fun validateMessage(sessionUserId: String, blockList: Set<String>, postType: PostType, userId: String) =
        sessionUserId == userId || // message is from the cur user or
            (!blockList.contains(userId) && // user isnt in cur user's blocklist
            sessionState.filterTypes.postTypes.contains(postType) && // and post/user type exists in filters
            sessionState.filterTypes.userRoles.contains(ServerState.userList[userId]?.roles ?: UserRole.GUEST))


    private suspend fun WebSocketServerSession.joinChat(user: User) {
        var sessionUser = user.copy()
        sendTypedMessage(MessageType.USER, usersApiRef.getUserById(sessionUser.id))

        val messageCollectionJobs = ServerState.flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> {
                            (message as? MessageDomain)?.let{ newMessage ->
                                if (validateMessage(sessionUser.id, sessionUser.blockList, newMessage.type, newMessage.userId)) {
                                    sendTypedMessage(type, newMessage)
                                }
                            }
                        }
                        MessageType.VOTE -> {
                            (message as? VoteDomain)?.let { newVote ->
                                if (sessionState.filterTypes.postTypes.contains(newVote.type)) {
                                    sendTypedMessage(type, newVote)
                                }
                            }
                        }
                        MessageType.DELETE -> sendTypedMessage(type, message as DeleteDomain)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_LOGIN -> {
                            val userLogin = message as? UserDomain
                            if (sessionUser.id == userLogin?.id) {
                                sendTypedMessage(MessageType.USER, userLogin)
                            } else {
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
                                    postsApiRef.putPost(messageWithData)
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
                                if (
                                    !sessionUser.banned &&
                                    ServerState.userList[dmWithData.userReceiveId]?.blockList?.contains(sessionUser.id) == false
                                ) {
                                    userToSocket[dmWithData.userReceiveId]?.sendTypedMessage(MessageType.DM, dmWithData)
                                    usersApiRef.putDM(dmWithData)
                                }
                            }
                        }
                    }
                    MessageType.VOTE -> {
                        Json.decodeFromString<VoteDomain>(messageWithType.value).copy(userId = sessionUser.id).let{vote ->
                            if (sessionUser.banned) {
                                sendTypedMessage(MessageType.VOTE, vote)
                            } else {
                                ServerState.voteFlowMut.emit(vote)
                                postsApiRef.votePost(vote)
                            }
                        }
                    }
                    MessageType.FILTER_TYPES -> {
                        Json.decodeFromString<FilterTypesDomain>(messageWithType.value).let{newFilterTypes ->
                            sessionState.filterTypes = newFilterTypes
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
                    MessageType.BAN -> {
                        val ban = Json.decodeFromString<BanDomain>(messageWithType.value)
                        if (sessionUser.roles == UserRole.ADMIN) {
                            ServerState.banUserFlowMut.emit(ban)
                            ServerState.banList.add(ban.userId)
                        }
                    }
                    MessageType.BLOCK -> {
                        val block = Json.decodeFromString<BlockDomain>(messageWithType.value)
                        val updateBlockList = sessionUser.blockList.toMutableSet()
                        updateBlockList.add(block.userId)
                        ServerState.userList[sessionUser.id] = sessionUser.copy(blockList = updateBlockList).toDomain()
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

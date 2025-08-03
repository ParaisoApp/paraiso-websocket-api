package com.paraiso.server.plugins

import com.paraiso.com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.com.paraiso.server.plugins.jobs.SportJobs
import com.paraiso.com.paraiso.server.util.SessionState
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.SiteRoute
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.users.buildUserResponse
import com.paraiso.domain.users.newUser
import com.paraiso.domain.users.toUser
import com.paraiso.domain.users.toUserResponse
import com.paraiso.domain.util.ServerState
import com.paraiso.server.util.cleanDirectMessage
import com.paraiso.server.util.cleanMessage
import com.paraiso.server.util.determineMessageType
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
import java.util.UUID
import com.paraiso.domain.messageTypes.Ban as BanDomain
import com.paraiso.domain.messageTypes.Block as BlockDomain
import com.paraiso.domain.messageTypes.Delete as DeleteDomain
import com.paraiso.domain.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.messageTypes.FilterTypes as FilterTypesDomain
import com.paraiso.domain.messageTypes.Follow as FollowDomain
import com.paraiso.domain.messageTypes.Message as MessageDomain
import com.paraiso.domain.messageTypes.Route as RouteDomain
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.messageTypes.Vote as VoteDomain
import com.paraiso.domain.users.UserResponse as UserResponseDomain

class WebSocketHandler(usersApi: UsersApi, postsApi: PostsApi) : Klogging {
    // jobs
    private val homeJobs = HomeJobs()
    private val profileJobs = ProfileJobs()
    private val sportJobs = SportJobs()

    // users
    private val userToSocket: MutableMap<String, WebSocketServerSession> = mutableMapOf()
    private val usersApiRef = usersApi

    // posts
    private val postsApiRef = postsApi

    // session state
    private val sessionState = SessionState()

    suspend fun handleUser(session: WebSocketServerSession) {
        // check cookies to see if existing user
        ServerState.userList[session.call.request.cookies["guest_id"] ?: ""]?.let { currentUser ->
            currentUser.buildUserResponse().copy(
                status = UserStatus.CONNECTED
            ).let { reconnectUser ->
                ServerState.userList[reconnectUser.id] = reconnectUser.toUser()
                userToSocket[reconnectUser.id] = session // map userid to socket
                session.joinChat(reconnectUser)
            }
        } ?: run { // otherwise generate guest
            UUID.randomUUID().toString().let { id ->
                val currentUser = UserResponseDomain.newUser(id)
                ServerState.userList[id] = currentUser.toUser()
                userToSocket[id] = session // map userid to socket
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
            else -> TODO()
        }
    }

    private fun validateMessage(sessionUserId: String, blockList: Set<String>, postType: PostType, userId: String) =
        sessionUserId == userId || // message is from the cur user or
            (
                !blockList.contains(userId) && // user isnt in cur user's blocklist
                    sessionState.filterTypes.postTypes.contains(postType) && // and post/user type exists in filters
                    sessionState.filterTypes.userRoles.contains(ServerState.userList[userId]?.roles ?: UserRole.GUEST)
                )

    private suspend fun WebSocketServerSession.joinChat(user: UserResponseDomain) {
        var sessionUser = user.copy()
        sendTypedMessage(MessageType.USER, sessionUser)

        val messageCollectionJobs = ServerState.flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> {
                            (message as? MessageDomain)?.let { newMessage ->
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
                        MessageType.FOLLOW -> sendTypedMessage(type, message as FollowDomain)
                        MessageType.DELETE -> sendTypedMessage(type, message as DeleteDomain)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_UPDATE -> sendTypedMessage(type, message as UserResponseDomain)
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

        ServerState.userUpdateFlowMut.emit(sessionUser)
        // holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageType = determineMessageType(frame)
                when (messageType) {
                    MessageType.MSG -> {
                        converter?.findCorrectConversion<TypeMappingDomain<MessageDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { message ->
                                UUID.randomUUID().toString().let { messageId ->
                                    cleanMessage(message).copy(
                                        id = messageId,
                                        userId = sessionUser.id,
                                        rootId = messageId.takeIf { message.rootId == "-1" } ?: message.rootId
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
                    }
                    MessageType.DM -> {
                        converter?.findCorrectConversion<TypeMappingDomain<DirectMessageDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { dm ->
                            cleanDirectMessage(dm).copy(
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
                    MessageType.FOLLOW -> {
                        converter?.findCorrectConversion<TypeMappingDomain<FollowDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(followerId = sessionUser.id)?.let { follow ->
                            if (sessionUser.banned) {
                                sendTypedMessage(MessageType.FOLLOW, follow)
                            } else {
                                ServerState.followFlowMut.emit(follow)
                                usersApiRef.follow(follow)
                            }
                        }
                    }
                    MessageType.VOTE -> {
                        converter?.findCorrectConversion<TypeMappingDomain<VoteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(voterId = sessionUser.id)?.let { vote ->
                            if (sessionUser.banned) {
                                sendTypedMessage(MessageType.VOTE, vote)
                            } else {
                                ServerState.voteFlowMut.emit(vote)
                                postsApiRef.votePost(vote)
                            }
                        }
                    }
                    MessageType.USER_UPDATE -> {
                        converter?.findCorrectConversion<TypeMappingDomain<UserResponseDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(id = sessionUser.id)?.let{userUpdate ->
                                ServerState.userUpdateFlowMut.emit(userUpdate)
                                usersApiRef.saveUser(userUpdate)
                            }
                    }
                    MessageType.FILTER_TYPES -> {
                        converter?.findCorrectConversion<TypeMappingDomain<FilterTypesDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { newFilterTypes ->
                            sessionState.filterTypes = newFilterTypes
                        }
                    }
                    MessageType.DELETE -> {
                        converter?.findCorrectConversion<TypeMappingDomain<DeleteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(userId = sessionUser.id)?.let{delete ->
                                ServerState.deleteFlowMut.emit(delete)
                                postsApiRef.deletePost(delete)
                            }
                    }
                    MessageType.BAN -> {
                        converter?.findCorrectConversion<TypeMappingDomain<BanDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { ban ->
                                if (sessionUser.roles == UserRole.ADMIN) {
                                    ServerState.banUserFlowMut.emit(ban)
                                    ServerState.banList.add(ban.userId)
                                }
                            }
                    }
                    MessageType.BLOCK -> {
                        converter?.findCorrectConversion<TypeMappingDomain<BlockDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { block ->
                                val updateBlockList = sessionUser.blockList.toMutableSet()
                                updateBlockList.add(block.userId)
                                ServerState.userList[sessionUser.id] =
                                    sessionUser.copy(blockList = updateBlockList).toUser()
                            }
                    }
                    MessageType.ROUTE -> {
                        converter?.findCorrectConversion<TypeMappingDomain<RouteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { route ->
                                activeJobs?.cancelAndJoin()
                                val session = this
                                activeJobs = launch {
                                    handleRoute(route, session)
                                }
                            }
                    }
                    else -> logger.error { "Invalid message type received $frame" }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Error parsing incoming data" }
        } finally {
            messageCollectionJobs.forEach { it.cancelAndJoin() }
            activeJobs?.cancelAndJoin()
            ServerState.userList[sessionUser.id]?.copy(
                status = UserStatus.DISCONNECTED,
                lastSeen = System.currentTimeMillis(),
                updatedOn = Clock.System.now()
            )?.let { userDisconnected ->
                ServerState.userUpdateFlowMut.emit(userDisconnected.buildUserResponse())
                ServerState.userList[sessionUser.id] = userDisconnected
            }
            userToSocket[sessionUser.id]?.close()
        }
    }
}

package com.paraiso.server.plugins

import com.paraiso.com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.com.paraiso.server.plugins.jobs.sports.BBallJobs
import com.paraiso.com.paraiso.server.plugins.jobs.sports.FBallJobs
import com.paraiso.com.paraiso.server.util.SessionState
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.domain.sport.sports.fball.FBallApi
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSessionResponse
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.users.newUser
import com.paraiso.domain.users.toDomain
import com.paraiso.domain.util.ServerState
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.util.cleanAndType
import com.paraiso.server.util.determineMessageType
import com.paraiso.server.util.getMentions
import com.paraiso.server.util.sendTypedMessage
import com.paraiso.server.util.validateUser
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.websocket.close
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.paraiso.domain.messageTypes.Ban as BanDomain
import com.paraiso.domain.messageTypes.Delete as DeleteDomain
import com.paraiso.domain.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.messageTypes.FilterTypes as FilterTypesDomain
import com.paraiso.domain.messageTypes.Follow as FollowDomain
import com.paraiso.domain.messageTypes.Message as MessageDomain
import com.paraiso.domain.messageTypes.Report as ReportDomain
import com.paraiso.domain.messageTypes.Tag as TagDomain
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.messageTypes.Vote as VoteDomain
import com.paraiso.domain.routes.Favorite as FavoriteDomain
import com.paraiso.domain.routes.Route as RouteDomain
import com.paraiso.domain.users.UserResponse as UserResponseDomain

class WebSocketHandler(
    private val serverId: String,
    private val eventServiceImpl: EventServiceImpl,
    private val userSessions: ConcurrentHashMap<String, Set<WebSocketServerSession>>,
    private val usersApi: UsersApi,
    private val userChatsApi: UserChatsApi,
    private val postsApi: PostsApi,
    private val adminApi: AdminApi,
    private val routesApi: RoutesApi,
    private val bBallApi: BBallApi,
    private val fBallApi: FBallApi
) : Klogging {

    suspend fun handleUser(
        session: WebSocketServerSession
    ) = coroutineScope {
        val sessionId = UUID.randomUUID().toString()
        // session state
        val sessionState = SessionState()
        // check cookies to see if existing user
        val currentUser = usersApi.getUserById(session.call.request.cookies["guest_id"] ?: "") ?:
            UserResponseDomain.newUser(UUID.randomUUID().toString())
        launch{
            //create or update session connected status
            eventServiceImpl.getUserSession(currentUser.id)?.let { existingSession ->
                eventServiceImpl.saveUserSession(
                    existingSession.copy(
                        sessionIds = existingSession.sessionIds + sessionId
                    )
                )
            } ?: run {
                eventServiceImpl.saveUserSession(
                    UserSessionResponse(
                        id = UUID.randomUUID().toString(),
                        userId = currentUser.id,
                        serverId = serverId,
                        status = UserStatus.CONNECTED,
                        sessionIds = setOf(sessionId)
                    ).toDomain()
                )
            }
        }
        launch {
            usersApi.saveUser(currentUser)
        }
        val curUserSessions = userSessions[currentUser.id] ?: emptySet()
        userSessions[currentUser.id] = curUserSessions + session

        session.joinChat(currentUser, sessionId, sessionState)
    }

    private suspend fun handleRoute(route: RouteDomain, session: WebSocketServerSession): List<Job> = coroutineScope {
        when (route.route) {
            SiteRoute.HOME -> HomeJobs().homeJobs(session)
            SiteRoute.PROFILE -> ProfileJobs().profileJobs(route.content, session)
            SiteRoute.SPORT -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> BBallJobs(bBallApi).sportJobs(session)
                    SiteRoute.FOOTBALL -> FBallJobs(fBallApi).sportJobs(session)
                    else -> {
                        logger.error("Unrecognized Sport: $route")
                        emptyList()
                    }
                }
            }
            SiteRoute.TEAM -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> BBallJobs(bBallApi).teamJobs(route.content, session)
                    SiteRoute.FOOTBALL -> FBallJobs(fBallApi).teamJobs(route.content, session)
                    else -> {
                        logger.error("Unrecognized Team: $route")
                        emptyList()
                    }
                }
            }
            else -> {
                logger.error("Unrecognized Route: $route")
                emptyList()
            }
        }
    }

    private suspend fun validateMessage(
        sessionUserId: String,
        blockList: Map<String, Boolean>,
        postType: PostType, userId: String?,
        sessionState: SessionState
    ) =
        sessionUserId == userId || // message is from the cur user or
            (
                !blockList.contains(userId) && // user isnt in cur user's blocklist
                    sessionState.filterTypes.postTypes.contains(postType) && // and post/user type exists in filters
                    userId != null &&
                    sessionState.filterTypes.userRoles.contains(
                        usersApi.getUserById(userId)?.roles ?: UserRole.GUEST
                    )
                )

    private suspend fun WebSocketServerSession.joinChat(
        incomingUser: UserResponseDomain,
        sessionId: String,
        sessionState: SessionState
    ) {
        var sessionUser = incomingUser.copy()
        sendTypedMessage(MessageType.USER, sessionUser)

        val messageCollectionJobs = ServerState.flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> {
                            (message as? MessageDomain)?.let { newMessage ->
                                if (
                                    validateMessage(
                                        sessionUser.id,
                                        sessionUser.blockList,
                                        newMessage.type,
                                        newMessage.userId,
                                        sessionState
                                    )
                                ) {
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
                        MessageType.REPORT_USER -> sendTypedMessage(type, message as ReportDomain)
                        MessageType.REPORT_POST -> sendTypedMessage(type, message as ReportDomain)
                        MessageType.TAG -> sendTypedMessage(type, message as TagDomain)
                        MessageType.FAVORITE -> sendTypedMessage(type, message as FavoriteDomain)
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
        eventServiceImpl.publish("${MessageType.USER_UPDATE.name}:$serverId:", Json.encodeToString(sessionUser))
        // holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageType = determineMessageType(frame)
                when (messageType) {
                    MessageType.MSG -> {
                        converter?.cleanAndType<TypeMappingDomain<MessageDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { message ->
                                UUID.randomUUID().toString().let { messageId ->
                                    val userIdMentions = usersApi.addMentions(
                                        getMentions(message.content),
                                        message.userReceiveIds.firstOrNull(),
                                        messageId
                                    )
                                    message.copy(
                                        id = messageId,
                                        userId = sessionUser.id,
                                        rootId = messageId.takeIf { message.rootId == null } ?: message.rootId,
                                        userReceiveIds = message.userReceiveIds.plus(userIdMentions)
                                    ).let { messageWithData ->
                                        if (sessionUser.banned) {
                                            sendTypedMessage(MessageType.MSG, messageWithData)
                                        } else {
                                            launch { postsApi.putPost(messageWithData) }
                                            launch { usersApi.putPost(sessionUser.id, messageId) }
                                            ServerState.messageFlowMut.emit(messageWithData)
                                            eventServiceImpl.publish("${MessageType.MSG.name}:$serverId:", Json.encodeToString(messageWithData))
                                        }
                                    }
                                }
                            }
                    }
                    MessageType.DM -> {
                        converter?.cleanAndType<TypeMappingDomain<DirectMessageDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { dm ->
                                dm.copy(
                                    id = UUID.randomUUID().toString(),
                                    userId = sessionUser.id
                                ).let { dmWithData ->
                                    launch { sendTypedMessage(MessageType.DM, dmWithData) }
                                    val userReceiveBlocklist = usersApi.getUserById(dmWithData.userReceiveId)?.blockList
                                    if (
                                        !sessionUser.banned &&
                                        userReceiveBlocklist?.contains(sessionUser.id) == false
                                    ) {
                                        launch {
                                            usersApi.updateChatForUser(
                                                dmWithData,
                                                dmWithData.userId,
                                                dmWithData.userReceiveId,
                                                true
                                            )
                                        } // update chat for receiving user
                                        launch {
                                            usersApi.updateChatForUser(
                                                dmWithData,
                                                dmWithData.userReceiveId,
                                                dmWithData.userId,
                                                false
                                            )
                                            // update chat for receiving user
                                            launch { userChatsApi.putDM(dmWithData) }
                                        }
                                        //if user is on this server then grab session and send dm to user
                                        userSessions[dmWithData.userReceiveId]?.let {receiveUserSessions ->
                                            receiveUserSessions.forEach { session ->
                                                session.sendTypedMessage(MessageType.DM, dmWithData)
                                            }
                                        }
                                        //find any other user server sessions, publish, and map to respective server subscriber
                                        eventServiceImpl.getUserSession(dmWithData.userReceiveId)?.let {receiveUserSessions ->
                                            val dmString = Json.encodeToString(dmWithData)
                                            receiveUserSessions.sessionIds.forEach { userServerId ->
                                                eventServiceImpl.publish(
                                                    "server:$userServerId",
                                                    "${dmWithData.userReceiveId}:${dmString}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    }
                    MessageType.FOLLOW -> {
                        converter?.cleanAndType<TypeMappingDomain<FollowDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(followerId = sessionUser.id)?.let { follow ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.FOLLOW, follow)
                                } else {
                                    launch { usersApi.follow(follow) }
                                    ServerState.followFlowMut.emit(follow)
                                    eventServiceImpl.publish("${MessageType.FOLLOW.name}:$serverId:", Json.encodeToString(follow))
                                }
                            }
                    }
                    MessageType.FAVORITE -> {
                        converter?.cleanAndType<TypeMappingDomain<FavoriteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(userId = sessionUser.id)?.let { favorite ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.FAVORITE, favorite)
                                } else {
                                    launch { usersApi.toggleFavoriteRoute(favorite) }
                                    launch { routesApi.toggleFavoriteRoute(favorite) }
                                    ServerState.favoriteFlowMut.emit(favorite)
                                    eventServiceImpl.publish("${MessageType.FAVORITE.name}:$serverId:", Json.encodeToString(favorite))
                                }
                            }
                    }
                    MessageType.VOTE -> {
                        converter?.cleanAndType<TypeMappingDomain<VoteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(voterId = sessionUser.id)?.let { vote ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.VOTE, vote)
                                } else {
                                    launch { postsApi.votePost(vote) }
                                    //ServerState.voteFlowMut.emit(vote)
                                    eventServiceImpl.publish("${MessageType.VOTE.name}:$serverId:", Json.encodeToString(vote))
                                }
                            }
                    }
                    MessageType.USER_UPDATE -> {
                        converter?.cleanAndType<TypeMappingDomain<UserResponseDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(id = sessionUser.id)?.let { user ->
                                if (user.validateUser()) {
                                    launch { usersApi.saveUser(user) }
                                    ServerState.userUpdateFlowMut.emit(user)
                                    eventServiceImpl.publish("${MessageType.USER_UPDATE.name}:$serverId:", Json.encodeToString(user))
                                }
                            }
                    }
                    MessageType.FILTER_TYPES -> {
                        converter?.cleanAndType<TypeMappingDomain<FilterTypesDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { newFilterTypes ->
                                sessionState.filterTypes = newFilterTypes
                            }
                    }
                    MessageType.DELETE -> {
                        converter?.cleanAndType<TypeMappingDomain<DeleteDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { delete ->
                                launch { postsApi.deletePost(delete, sessionUser.id) }
                                ServerState.deleteFlowMut.emit(delete)
                                eventServiceImpl.publish("${MessageType.DELETE.name}:$serverId:", Json.encodeToString(delete))
                            }
                    }
                    MessageType.BAN -> {
                        converter?.cleanAndType<TypeMappingDomain<BanDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { ban ->
                                if (sessionUser.roles == UserRole.ADMIN) {
                                    launch { usersApi.banUser(ban) }
                                    ServerState.banUserFlowMut.emit(ban)
                                    eventServiceImpl.publish("${MessageType.BAN.name}:$serverId:", Json.encodeToString(ban))
                                }
                            }
                    }
                    MessageType.TAG -> {
                        converter?.cleanAndType<TypeMappingDomain<TagDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { tag ->
                                if (sessionUser.roles == UserRole.ADMIN) {
                                    launch { usersApi.tagUser(tag) }
                                    ServerState.tagUserFlowMut.emit(tag)
                                    eventServiceImpl.publish("${MessageType.TAG.name}:$serverId:", Json.encodeToString(tag))
                                }
                            }
                    }
                    MessageType.REPORT_USER -> {
                        converter?.cleanAndType<TypeMappingDomain<ReportDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { reportUser ->
                                launch { adminApi.reportUser(sessionUser.id, reportUser) }
                                launch { usersApi.addUserReport(reportUser) }
                                ServerState.reportUserFlowMut.emit(reportUser)
                                eventServiceImpl.publish("${MessageType.REPORT_USER.name}:$serverId:", Json.encodeToString(reportUser))
                            }
                    }
                    MessageType.REPORT_POST -> {
                        converter?.cleanAndType<TypeMappingDomain<ReportDomain>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { reportPost ->
                                launch { adminApi.reportPost(sessionUser.id, reportPost) }
                                launch { usersApi.addPostReport(reportPost) }
                                ServerState.reportPostFlowMut.emit(reportPost)
                                eventServiceImpl.publish("${MessageType.REPORT_POST.name}:$serverId:", Json.encodeToString(reportPost))
                            }
                    }
                    MessageType.ROUTE -> {
                        converter?.cleanAndType<TypeMappingDomain<RouteDomain>>(frame)
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
            //create or update session connected status
            eventServiceImpl.getUserSession(sessionUser.id)?.let { existingSession ->
                val remainingSessions = existingSession.sessionIds - sessionId
                if(remainingSessions.isEmpty()){
                    eventServiceImpl.deleteUserSession(sessionUser.id)
                } else {
                    eventServiceImpl.saveUserSession(
                        existingSession.copy(
                            sessionIds = remainingSessions
                        )
                    )
                }
            }
            usersApi.getUserById(sessionUser.id)?.let { userDisconnected ->
                ServerState.userUpdateFlowMut.emit(userDisconnected)
                eventServiceImpl.publish("${MessageType.USER_UPDATE.name}:$serverId:", Json.encodeToString(userDisconnected))
                //remove current user session from sessions map
                val curUserSessions = userSessions[userDisconnected.id]?.minus(this) ?: emptySet()
                //if user has no more sessions, remove user from server user sessions
                if(curUserSessions.isEmpty()) userSessions.remove(userDisconnected.id)
                this.close()
            }
        }
    }
}

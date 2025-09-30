package com.paraiso.server.plugins

import com.paraiso.AppServices
import com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.server.plugins.jobs.SportJobs
import com.paraiso.server.util.SessionState
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.TypeMapping
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.VoteResponse
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.routes.Route
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSessionResponse
import com.paraiso.domain.users.UserStatus
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
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WebSocketHandler(
    private val serverId: String,
    private val eventServiceImpl: EventServiceImpl,
    private val userSessions: ConcurrentHashMap<String, Set<WebSocketServerSession>>,
    private val services: AppServices
) : Klogging {

    suspend fun handleUser(
        session: WebSocketServerSession
    ) = coroutineScope {
        val sessionId = UUID.randomUUID().toString()
        // session state
        val sessionState = SessionState()
        // check cookies to see if existing user
        val checkExistingUser = services.userSessionsApi.getUserById(session.call.request.cookies["guest_id"] ?: "")
        val currentUser = checkExistingUser ?: UserResponse.newUser(UUID.randomUUID().toString())
        launch{
            // new user so create new route entry
            if(checkExistingUser == null){
                val now = Clock.System.now()
                services.routesApi.saveRoutes(
                    listOf(
                        RouteDetails(
                            id = "/p/${currentUser.id}",
                            route = SiteRoute.PROFILE,
                            modifier = null,
                            title = currentUser.name ?: "UNKNOWN",
                            userFavorites = emptySet(),
                            about = null,
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
            }
        }
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
            services.usersApi.saveUser(currentUser)
        }
        val curUserSessions = userSessions[currentUser.id] ?: emptySet()
        userSessions[currentUser.id] = curUserSessions + session

        session.joinChat(currentUser, sessionId, sessionState)
    }

    private suspend fun WebSocketServerSession.joinChat(
        incomingUser: UserResponse,
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
                            (message as? Message)?.let { newMessage ->
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
                            (message as? VoteResponse)?.let { newVote ->
                                if (sessionState.filterTypes.postTypes.contains(newVote.type)) {
                                    sendTypedMessage(type, newVote)
                                }
                            }
                        }
                        MessageType.FOLLOW -> sendTypedMessage(type, message as Follow)
                        MessageType.DELETE -> sendTypedMessage(type, message as Delete)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_UPDATE -> sendTypedMessage(
                            type, //remove private data from user update socket messages
                            (message as UserResponse).copy(
                                userReports = emptyMap(),
                                postReports = emptyMap(),
                                banned = false,
                                blockList = emptyMap(),
                            )
                        )
                        MessageType.REPORT_USER -> sendTypedMessage(type, message as Report)
                        MessageType.REPORT_POST -> sendTypedMessage(type, message as Report)
                        MessageType.TAG -> sendTypedMessage(type, message as Tag)
                        MessageType.FAVORITE -> sendTypedMessage(type, message as Favorite)
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

        ServerState.userUpdateFlowMut.emit(sessionUser)
        eventServiceImpl.publish(MessageType.USER_UPDATE.name, "$serverId:${Json.encodeToString(sessionUser)}")
        this.parseAndRouteMessages(sessionUser, sessionId, sessionState, messageCollectionJobs)
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
                            services.userSessionsApi.getUserById(userId)?.roles ?: UserRole.GUEST
                        )
                )

    private suspend fun WebSocketServerSession.parseAndRouteMessages(
        sessionUser: UserResponse,
        sessionId: String,
        sessionState: SessionState,
        messageCollectionJobs: List<Job>
    ) {
        // holds the active jobs for given route
        var activeJobs: Job? = null
        try {
            incoming.consumeEach { frame ->
                val messageType = determineMessageType(frame)
                when (messageType) {
                    MessageType.MSG -> {
                        converter?.cleanAndType<TypeMapping<Message>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { message ->
                                val messageId: String = message.editId ?: UUID.randomUUID().toString()
                                val userIdMentions = services.usersApi.addMentions(
                                    getMentions(message.content),
                                    message.userReceiveIds.firstOrNull(),
                                    messageId,
                                    message.userId
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
                                        launch { services.postsApi.putPost(messageWithData) }
                                        ServerState.messageFlowMut.emit(messageWithData)
                                        eventServiceImpl.publish(MessageType.MSG.name, "$serverId:${Json.encodeToString(messageWithData)}")
                                    }
                                }
                            }
                    }
                    MessageType.DM -> {
                        converter?.cleanAndType<TypeMapping<DirectMessage>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { dm ->
                                dm.copy(
                                    id = UUID.randomUUID().toString(),
                                    userId = sessionUser.id
                                ).let { dmWithData ->
                                    launch { sendTypedMessage(MessageType.DM, dmWithData) }
                                    val userReceiveBlocklist = services.userSessionsApi.getUserById(dmWithData.userReceiveId)?.blockList
                                    if (
                                        !sessionUser.banned &&
                                        userReceiveBlocklist?.contains(sessionUser.id) == false
                                    ) {
                                        launch {
                                            services.usersApi.updateChatForUser(
                                                dmWithData,
                                                dmWithData.userId,
                                                dmWithData.userReceiveId,
                                                true
                                            )
                                        } // update chat for receiving user
                                        launch {
                                            services.usersApi.updateChatForUser(
                                                dmWithData,
                                                dmWithData.userReceiveId,
                                                dmWithData.userId,
                                                false
                                            )
                                            // update chat for receiving user
                                            launch { services.userChatsApi.putDM(dmWithData) }
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
                                                eventServiceImpl.publish("server:$userServerId", "$serverId:${dmWithData.userReceiveId}:${dmString}")
                                            }
                                        }
                                    }
                                }
                            }
                    }
                    MessageType.FOLLOW -> {
                        converter?.cleanAndType<TypeMapping<Follow>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(followerId = sessionUser.id)?.let { follow ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.FOLLOW, follow)
                                } else {
                                    launch { services.usersApi.follow(follow) }
                                    ServerState.followFlowMut.emit(follow)
                                    eventServiceImpl.publish(MessageType.FOLLOW.name, "$serverId:${Json.encodeToString(follow)}")
                                }
                            }
                    }
                    MessageType.FAVORITE -> {
                        converter?.cleanAndType<TypeMapping<Favorite>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(userId = sessionUser.id)?.let { favorite ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.FAVORITE, favorite)
                                } else {
                                    launch { services.usersApi.toggleFavoriteRoute(favorite) }
                                    launch { services.routesApi.toggleFavoriteRoute(favorite) }
                                    ServerState.favoriteFlowMut.emit(favorite)
                                    eventServiceImpl.publish(MessageType.FAVORITE.name, "$serverId:${Json.encodeToString(favorite)}")
                                }
                            }
                    }
                    MessageType.VOTE -> {
                        converter?.cleanAndType<TypeMapping<VoteResponse>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(voterId = sessionUser.id)?.let { vote ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.VOTE, vote)
                                } else {
                                    val score = services.votesApi.vote(vote)
                                    launch { services.postsApi.votePost(vote.postId, score) }
                                    launch {
                                        vote.voteeId?.let{voteeId ->
                                            services.usersApi.votePost(voteeId, score)
                                        }
                                    }
                                    ServerState.voteFlowMut.emit(vote.copy(score = score))
                                    eventServiceImpl.publish(MessageType.VOTE.name, "$serverId:${Json.encodeToString(vote)}")
                                }
                            }
                    }
                    MessageType.USER_UPDATE -> {
                        converter?.cleanAndType<TypeMapping<UserResponse>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(id = sessionUser.id)?.let { user ->
                                if (user.validateUser()) {
                                    launch { services.usersApi.saveUser(user) }
                                    ServerState.userUpdateFlowMut.emit(user)
                                    eventServiceImpl.publish(MessageType.USER_UPDATE.name, "$serverId:${Json.encodeToString(user)}")
                                }
                            }
                    }
                    MessageType.FILTER_TYPES -> {
                        converter?.cleanAndType<TypeMapping<FilterTypes>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { newFilterTypes ->
                                sessionState.filterTypes = newFilterTypes
                            }
                    }
                    MessageType.DELETE -> {
                        converter?.cleanAndType<TypeMapping<Delete>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { delete ->
                                launch { services.postsApi.deletePost(delete, sessionUser.id) }
                                ServerState.deleteFlowMut.emit(delete)
                                eventServiceImpl.publish(MessageType.DELETE.name, "$serverId:${Json.encodeToString(delete)}")
                            }
                    }
                    MessageType.BAN -> {
                        converter?.cleanAndType<TypeMapping<Ban>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { ban ->
                                if (sessionUser.roles == UserRole.ADMIN) {
                                    launch { services.usersApi.banUser(ban) }
                                    ServerState.banUserFlowMut.emit(ban)
                                    eventServiceImpl.publish(MessageType.BAN.name, "$serverId:${Json.encodeToString(ban)}")
                                }
                            }
                    }
                    MessageType.TAG -> {
                        converter?.cleanAndType<TypeMapping<Tag>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { tag ->
                                if (sessionUser.roles == UserRole.ADMIN) {
                                    launch { services.usersApi.tagUser(tag) }
                                    ServerState.tagUserFlowMut.emit(tag)
                                    eventServiceImpl.publish(MessageType.TAG.name, "$serverId:${Json.encodeToString(tag)}")
                                }
                            }
                    }
                    MessageType.REPORT_USER -> {
                        converter?.cleanAndType<TypeMapping<Report>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { reportUser ->
                                launch { services.adminApi.reportUser(sessionUser.id, reportUser) }
                                launch { services.usersApi.addUserReport(reportUser) }
                                ServerState.reportUserFlowMut.emit(reportUser)
                                eventServiceImpl.publish(MessageType.REPORT_USER.name, "$serverId:${Json.encodeToString(reportUser)}")
                            }
                    }
                    MessageType.REPORT_POST -> {
                        converter?.cleanAndType<TypeMapping<Report>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { reportPost ->
                                launch { services.adminApi.reportPost(sessionUser.id, reportPost) }
                                launch { services.usersApi.addPostReport(reportPost) }
                                ServerState.reportPostFlowMut.emit(reportPost)
                                eventServiceImpl.publish(MessageType.REPORT_POST.name, "$serverId:${Json.encodeToString(reportPost)}")
                            }
                    }
                    MessageType.ROUTE -> {
                        converter?.cleanAndType<TypeMapping<Route>>(frame)
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
            services.userSessionsApi.getUserById(sessionUser.id)?.copy(status = UserStatus.DISCONNECTED)?.let { userDisconnected ->
                ServerState.userUpdateFlowMut.emit(userDisconnected)
                eventServiceImpl.publish(MessageType.USER_UPDATE.name, "$serverId:${Json.encodeToString(userDisconnected)}")
                //remove current user session from sessions map
                val curUserSessions = userSessions[userDisconnected.id]?.minus(this) ?: emptySet()
                //if user has no more sessions, remove user from server user sessions
                if(curUserSessions.isEmpty()) userSessions.remove(userDisconnected.id)
                this.close()
            }
        }
    }

    private suspend fun handleRoute(route: Route, session: WebSocketServerSession): List<Job> = coroutineScope {
        when (route.route) {
            SiteRoute.HOME -> HomeJobs().homeJobs(session)
            SiteRoute.PROFILE -> ProfileJobs().profileJobs(route.content, session)
            SiteRoute.SPORT -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> SportJobs().sportJobs(session, SiteRoute.BASKETBALL.name)
                    SiteRoute.FOOTBALL -> SportJobs().sportJobs(session, SiteRoute.FOOTBALL.name)
                    else -> {
                        logger.error("Unrecognized Sport: $route")
                        emptyList()
                    }
                }
            }
            SiteRoute.TEAM -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> SportJobs().teamJobs(route.content, session, SiteRoute.BASKETBALL.name)
                    SiteRoute.FOOTBALL -> SportJobs().teamJobs(route.content, session, SiteRoute.FOOTBALL.name)
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
}

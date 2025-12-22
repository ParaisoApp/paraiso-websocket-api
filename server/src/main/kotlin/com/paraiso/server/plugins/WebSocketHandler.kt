package com.paraiso.server.plugins

import com.paraiso.AppServices
import com.paraiso.domain.follows.FollowResponse
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Subscription
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.TypeMapping
import com.paraiso.domain.messageTypes.init
import com.paraiso.domain.notifications.NotificationResponse
import com.paraiso.domain.notifications.NotificationType
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.routes.Route
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.userchats.DirectMessageResponse
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSessionResponse
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.ViewerContext
import com.paraiso.domain.users.newUser
import com.paraiso.domain.users.toDomain
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.ServerState
import com.paraiso.domain.votes.VoteResponse
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.server.plugins.jobs.SportJobs
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WebSocketHandler(
    private val serverId: String,
    private val eventServiceImpl: EventServiceImpl,
    private val userSessions: ConcurrentHashMap<String, ConcurrentHashMap<String, SessionContext>>,
    private val services: AppServices
) : Klogging {

    suspend fun connect(
        session: WebSocketServerSession
    ) {
        val sessionContext = SessionContext(session)
        val existingUser = services.userSessionsApi.getUserById(
            session.call.request.cookies["guest_id"] ?: "",
            null
        )
        // check cookies to see if existing user
        val currentUser = (existingUser ?: UserResponse.newUser(UUID.randomUUID().toString()))
            .copy(
                status = UserStatus.CONNECTED,
                sessionId = sessionContext.sessionId
            )
        // Track this session in the local set
        val sessionsForUser = userSessions.computeIfAbsent(currentUser.id) { ConcurrentHashMap() }
        sessionsForUser[sessionContext.sessionId] = sessionContext
        session.handleUser(sessionContext.sessionId, currentUser, existingUser == null, sessionContext)
    }

    private suspend fun WebSocketServerSession.handleUser(
        sessionId: String,
        currentUser: UserResponse,
        newUser: Boolean,
        sessionContext: SessionContext
    ) {
        // new user so create new route entry
        launch {
            if (newUser) {
                val now = Clock.System.now()
                services.routesApi.saveRoutes(
                    listOf(
                        RouteDetails(
                            id = "/p/${currentUser.id}",
                            route = SiteRoute.PROFILE,
                            modifier = currentUser.id,
                            title = currentUser.name ?: "UNKNOWN",
                            userFavorites = 0,
                            about = null,
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
            }
        }
        // create or update session connected status
        launch {
            val sessionToSave = eventServiceImpl.getUserSession(currentUser.id)?.let { existingSession ->
                val serverSessions = existingSession.serverSessions.toMutableMap()
                serverSessions[serverId] = (serverSessions[serverId] ?: emptySet()) + sessionId
                existingSession.copy(
                    serverSessions = serverSessions
                )
            } ?: run {
                UserSessionResponse(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.id,
                    serverSessions = mapOf(serverId to setOf(sessionId)),
                    status = UserStatus.CONNECTED
                ).toDomain()
            }
            eventServiceImpl.saveUserSession(sessionToSave)
        }
        launch {
            services.usersApi.saveUser(currentUser)
        }

        joinChat(currentUser, sessionId, sessionContext)
    }

    private suspend fun WebSocketServerSession.joinChat(
        incomingUser: UserResponse,
        sessionId: String,
        sessionContext: SessionContext
    ) {
        var sessionUser = incomingUser.copy()
        sendTypedMessage(MessageType.USER, sessionUser)

        // setup message intake (from others)
        ServerState.flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> {
                            (message as? Message)?.let { newMessage ->
                                val block = services.blocksApi.findIn(
                                    sessionUser.id,
                                    listOf(newMessage.userId ?: "")
                                ).firstOrNull()
                                if (
                                    validateMessage(
                                        sessionUser.id,
                                        block?.blocking == true,
                                        newMessage,
                                        sessionContext
                                    )
                                ) {
                                    val mappedMessage = if (sessionContext.routeId == HOME_PREFIX && message.replyId == message.route) {
                                        message.copy(replyId = HOME_PREFIX)
                                    } else {
                                        message
                                    }
                                    sendTypedMessage(type, mappedMessage)
                                }
                            }
                        }
                        MessageType.VOTE -> {
                            (message as? VoteResponse)?.let { newVote ->
                                if (sessionContext.filterTypes.postTypes.contains(newVote.type)) {
                                    sendTypedMessage(type, newVote)
                                }
                            }
                        }
                        MessageType.FOLLOW -> sendTypedMessage(type, message as FollowResponse)
                        MessageType.DELETE -> sendTypedMessage(type, message as Delete)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.USER_UPDATE -> {
                            val user = (message as UserResponse)
                            val follow = services.followsApi.findIn(sessionUser.id, listOf(user.id)).firstOrNull()
                            val block = services.blocksApi.findIn(sessionUser.id, listOf(user.id)).firstOrNull()
                            sendTypedMessage(
                                type, // remove private data from user update socket messages
                                user.copy(
                                    reports = 0,
                                    banned = false,
                                    viewerContext = ViewerContext(
                                        following = follow?.following,
                                        blocking = block?.blocking
                                    )
                                )
                            )
                        }
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
        parseAndRouteMessages(sessionUser, sessionId, sessionContext)
    }

    private suspend fun validateMessage(
        sessionUserId: String,
        blocking: Boolean,
        message: Message,
        sessionContext: SessionContext
    ) = message.userId?.let { userId -> // user id not null condition
        val isUser = sessionUserId == userId
        val roles = services.userSessionsApi.getUserById(userId, null)?.roles ?: UserRole.GUEST
        // user is on home - take any message, otherwise message must come from route
        val routeCriteria = sessionContext.routeId == message.route
        val filterCriteria =
            sessionContext.filterTypes.userRoles.contains(roles) &&
                sessionContext.filterTypes.postTypes.contains(message.type) &&
                routeCriteria
        val messageCriteria = !blocking && filterCriteria // user isn't in cur user's block list
        isUser || messageCriteria
    } ?: false

    private suspend fun WebSocketServerSession.parseAndRouteMessages(
        sessionUser: UserResponse,
        sessionId: String,
        sessionContext: SessionContext
    ) {
        // holds the active jobs for given route
        val activeJobs = mutableListOf<Job>()
        val activeComps = ConcurrentHashMap<Set<String>, Job>()
        val activeBoxScores = ConcurrentHashMap<String, Job>()
        try {
            launch {
                try {
                    // Continuously collect messages pushed from the Redis listener
                    for (encodedMessage in sessionContext.inboundChannel) {
                        val messageType = determineMessageType(encodedMessage)
                        when (messageType) {
                            MessageType.SUBSCRIBE -> {
                                Json.decodeFromString<TypeMapping<Subscription>>(encodedMessage)
                                    .typeMapping.entries.first().value.let { route ->
                                        launch {
                                            subscribe(route, activeBoxScores, activeComps, sessionContext.session)
                                        }
                                    }
                            }
                            else -> logger.error { "Unexpected message received in session channel: $encodedMessage" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error { "Redis Collector Job failed: $e" }
                }
            }

            for (frame in incoming) {
                when (val messageType = determineMessageType(frame)) {
                    MessageType.MSG -> {
                        converter?.cleanAndType<TypeMapping<Message>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { message ->
                                handleMessage(message, sessionUser)
                            }
                    }
                    MessageType.DM -> {
                        converter?.cleanAndType<TypeMapping<DirectMessageResponse>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { dm ->
                                handleDm(dm, sessionUser)
                            }
                    }
                    MessageType.FOLLOW -> {
                        converter?.cleanAndType<TypeMapping<FollowResponse>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.copy(followerId = sessionUser.id)?.let { follow ->
                                if (sessionUser.banned) {
                                    sendTypedMessage(MessageType.FOLLOW, follow)
                                } else {
                                    launch { services.followsApi.follow(follow) }
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
                                    // indicates toggle on or off rather than adding route flair
                                    if (favorite.toggle) {
                                        launch { services.routesApi.toggleFavoriteRoute(favorite) }
                                    }
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
                                        vote.voteeId?.let { voteeId ->
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
                                sessionContext.filterTypes = newFilterTypes
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
                                launch { services.usersApi.addReport() }
                                ServerState.reportUserFlowMut.emit(reportUser)
                                eventServiceImpl.publish(MessageType.REPORT_USER.name, "$serverId:${Json.encodeToString(reportUser)}")
                            }
                    }
                    MessageType.REPORT_POST -> {
                        converter?.cleanAndType<TypeMapping<Report>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { reportPost ->
                                launch { services.adminApi.reportPost(sessionUser.id, reportPost) }
                                launch { services.usersApi.addReport() }
                                ServerState.reportPostFlowMut.emit(reportPost)
                                eventServiceImpl.publish(MessageType.REPORT_POST.name, "$serverId:${Json.encodeToString(reportPost)}")
                            }
                    }
                    MessageType.ROUTE -> {
                        converter?.cleanAndType<TypeMapping<Route>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { route ->
                                if (sessionContext.routeId != route.routeId) {
                                    // clear out any active subscriptions on full route change
                                    activeJobs.removeIf { job ->
                                        job.cancel()
                                        true
                                    }
                                    activeBoxScores.entries.removeIf {
                                            sub ->
                                        sub.value.cancel()
                                        true
                                    }
                                    activeComps.entries.removeIf {
                                            sub ->
                                        sub.value.cancel()
                                        true
                                    }
                                    activeJobs += launch {
                                        handleRoute(route, sessionContext.session)
                                    }
                                    sessionContext.routeId = route.routeId
                                }
                            }
                    }
                    MessageType.SUBSCRIBE -> {
                        converter?.cleanAndType<TypeMapping<Subscription>>(frame)
                            ?.typeMapping?.entries?.first()?.value?.let { subscription ->
                                launch {
                                    subscribe(subscription, activeBoxScores, activeComps, sessionContext.session)
                                }
                            }
                    }
                    MessageType.PING -> {}
                    else -> logger.error { "Unexpected message received from client: $frame, messageType: $messageType" }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Exception caught for user ID ${sessionUser.id} sessionId: $sessionId" }
        } finally {
            val session = this
            withContext(NonCancellable) {
                session.close()
                // create or update session connected status
                eventServiceImpl.getUserSession(sessionUser.id)?.let { existingSession ->
                    val serverSessions = existingSession.serverSessions.toMutableMap()
                    // Remove this sessionId from the current server’s set
                    val remainingSessions = (serverSessions[serverId] ?: emptySet()) - sessionId
                    if (remainingSessions.isEmpty()) {
                        serverSessions.remove(serverId)
                    } else {
                        serverSessions[serverId] = remainingSessions
                    }
                    val userDisconnected = services.userSessionsApi.getUserById(sessionUser.id, null)
                        ?.copy(status = UserStatus.DISCONNECTED)
                    if (serverSessions.isEmpty()) {
                        // Fully disconnected, remove from redis and publish out user disconnect
                        eventServiceImpl.deleteUserSession(sessionUser.id)
                        if (userDisconnected != null) {
                            ServerState.userUpdateFlowMut.emit(userDisconnected)
                            eventServiceImpl.publish(
                                MessageType.USER_UPDATE.name,
                                "$serverId:${Json.encodeToString(userDisconnected)}"
                            )
                        }
                    } else {
                        // Not fully disconnected so just update redis session list
                        eventServiceImpl.saveUserSession(existingSession.copy(serverSessions = serverSessions))
                    }
                    // remove user from local map if no sessions remain on server
                    userDisconnected?.id?.let { userId ->
                        userSessions.computeIfPresent(userId) { _, sessionsMap ->
                            sessionsMap.remove(sessionId)
                            sessionsMap.ifEmpty { null }
                        }
                    }
                }
            }
        }
    }

    private suspend fun WebSocketServerSession.handleMessage(
        message: Message,
        sessionUser: UserResponse
    ) = coroutineScope {
        val messageId: String = message.editId ?: UUID.randomUUID().toString()
        val userIdMentions = services.usersApi.addMentions(
            // retreive mentions from content (parse with @)
            getMentions(message.content),
            message.userReceiveIds.firstOrNull(),
            sessionUser.id
        )
        // create notifications for all mentions or replied users
        userIdMentions.map { userReceiveId ->
            val type = if (message.userReceiveIds.firstOrNull() == userReceiveId) {
                NotificationType.POST
            } else {
                NotificationType.MENTION
            }
            NotificationResponse(
                id = "$userReceiveId-${sessionUser.id}-$messageId-${message.replyId}",
                userId = userReceiveId,
                createUserId = sessionUser.id,
                refId = messageId,
                replyId = message.replyId,
                content = null,
                type = type,
                userRead = false,
                createdOn = Clock.System.now(),
                updatedOn = Clock.System.now()
            )
        }.let { notifications ->
            if (notifications.isNotEmpty()) {
                services.notificationsApi.save(notifications)
            }
        }
        // create vote for create user
        launch {
            services.votesApi.vote(
                VoteResponse(
                    voterId = sessionUser.id,
                    voteeId = sessionUser.id,
                    type = message.type,
                    postId = messageId,
                    upvote = true
                )
            )
        }
        // add init vote to user score
        launch {
            services.usersApi.votePost(
                sessionUser.id,
                1
            )
        }
        message.copy(
            id = messageId,
            userId = sessionUser.id,
            // if root id == message id then it's a post at the root of the route
            rootId = messageId.takeIf { message.rootId == null } ?: message.rootId,
            userReceiveIds = message.userReceiveIds.plus(userIdMentions)
        ).let { messageWithData ->
            // shadow send message if user banned
            if (sessionUser.banned) {
                sendTypedMessage(MessageType.MSG, messageWithData)
            } else {
                // emit to this server, publish downstream to other servers
                launch { services.postsApi.putPost(messageWithData) }
                ServerState.messageFlowMut.emit(messageWithData)
                eventServiceImpl.publish(MessageType.MSG.name, "$serverId:${Json.encodeToString(messageWithData)}")
            }
        }
    }
    private suspend fun WebSocketServerSession.handleDm(
        dm: DirectMessageResponse,
        sessionUser: UserResponse
    ) = coroutineScope {
        dm.copy(
            id = UUID.randomUUID().toString(),
            userId = sessionUser.id
        ).let { dmWithData ->
            launch { sendTypedMessage(MessageType.DM, dmWithData) }
            val userReceiveBlocking = services.blocksApi.findIn(
                dmWithData.userReceiveId,
                listOf(sessionUser.id)
            ).firstOrNull()?.blocking == true
            if (
                !sessionUser.banned &&
                !userReceiveBlocking
            ) {
                // update chat for receiving user
                launch {
                    if (dmWithData.userId != dmWithData.userReceiveId) {
                        services.usersApi.addChat(
                            dmWithData.userReceiveId
                        )
                    }
                }
                launch {
                    dmWithData.id?.let {
                        services.userChatsApi.setMostRecentDm(it, dmWithData.chatId)
                    }
                }
                launch { services.directMessagesApi.save(dmWithData) }
                // if user is on this server then grab session and send dm to user
                if (dmWithData.userReceiveId != dmWithData.userId) {
                    userSessions[dmWithData.userReceiveId]?.let { receiveUserSessions ->
                        receiveUserSessions.map { it.value.session }.forEach { session ->
                            session.sendTypedMessage(MessageType.DM, dmWithData)
                        }
                    }
                    // find any other user server sessions, publish, and map to respective server subscriber
                    eventServiceImpl.getUserSession(dmWithData.userReceiveId)?.let { receiveUserSessions ->
                        val dmString = Json.encodeToString(dmWithData)
                        receiveUserSessions.serverSessions.keys
                            .filter { it != serverId }
                            .forEach { server ->
                                eventServiceImpl.publish(
                                    "server:$server",
                                    "$server:${MessageType.DM}:$dmString"
                                )
                            }
                    }
                }
            }
        }
    }

    private suspend fun handleRoute(
        route: Route,
        session: WebSocketServerSession
    ): List<Job> = coroutineScope {
        when (route.route) {
            SiteRoute.HOME -> HomeJobs().homeJobs(session)
            SiteRoute.PROFILE -> ProfileJobs().profileJobs(route.content, session)
            SiteRoute.SPORT -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> SportJobs(services.sportApi).sportJobs(session, SiteRoute.BASKETBALL.name)
                    SiteRoute.FOOTBALL -> SportJobs(services.sportApi).sportJobs(session, SiteRoute.FOOTBALL.name)
                    SiteRoute.HOCKEY -> SportJobs(services.sportApi).sportJobs(session, SiteRoute.HOCKEY.name)
                    else -> {
                        logger.error("Unrecognized Sport: $route")
                        emptyList()
                    }
                }
            }
            SiteRoute.TEAM -> {
                when (route.modifier) {
                    SiteRoute.BASKETBALL -> SportJobs(services.sportApi).teamJobs(route.content, session, SiteRoute.BASKETBALL.name)
                    SiteRoute.FOOTBALL -> SportJobs(services.sportApi).teamJobs(route.content, session, SiteRoute.FOOTBALL.name)
                    SiteRoute.HOCKEY -> SportJobs(services.sportApi).teamJobs(route.content, session, SiteRoute.HOCKEY.name)
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

    private suspend fun subscribe(
        subscription: Subscription,
        activeBoxScores: ConcurrentHashMap<String, Job>,
        activeComps: ConcurrentHashMap<Set<String>, Job>,
        session: WebSocketServerSession
    ) = coroutineScope {
        when (subscription.type) {
            MessageType.BOX_SCORES -> {
                if (subscription.subscribe) {
                    SportJobs(services.sportApi).boxScoreJobs(subscription.ids, activeBoxScores, session)
                } else {
                    subscription.ids.forEach { id ->
                        activeBoxScores[id]?.cancel()
                        activeBoxScores.remove(id)
                    }
                }
            }
            MessageType.COMPS -> {
                val compJob = activeComps.entries.firstOrNull()
                if (subscription.subscribe) {
                    val existingIds = compJob?.key ?: emptySet()
                    // if any incoming ids don't exist in current set then restart with intersection of sets
                    if (!existingIds.containsAll(subscription.ids)) {
                        compJob?.value?.cancel()
                        compJob?.let { activeComps.remove(it.key) }
                        val allIds = existingIds + subscription.ids
                        val newCompJob = launch {
                            SportJobs(services.sportApi).competitionJobs(allIds, session)
                        }
                        activeComps[allIds] = newCompJob
                    } else {
                        null
                    }
                } else {
                    compJob?.value?.cancel()
                    activeComps.remove(compJob?.key)
                }
            }
            else -> {
                logger.error("Unrecognized subscription type: ${subscription.type}")
            }
        }
    }
}
data class SessionContext(
    val session: WebSocketServerSession,
    val sessionId: String = UUID.randomUUID().toString(),
    val inboundChannel: Channel<String> = Channel(Channel.UNLIMITED),
    var routeId: String? = null,
    var filterTypes: FilterTypes = FilterTypes.init()
)

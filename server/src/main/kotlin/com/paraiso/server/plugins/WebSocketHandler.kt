package com.paraiso.server.plugins

import com.paraiso.AppServices
import com.paraiso.domain.follows.Follow
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.messageTypes.RouteUpdate
import com.paraiso.domain.messageTypes.ServerEvent
import com.paraiso.domain.messageTypes.Subscription
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.TypeMapping
import com.paraiso.domain.messageTypes.init
import com.paraiso.domain.notifications.Notification
import com.paraiso.domain.notifications.NotificationType
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.routes.Route
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.userchats.DirectMessage
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSession
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.ViewerContext
import com.paraiso.domain.users.toBasicResponse
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.USER_PREFIX
import com.paraiso.domain.util.ServerState
import com.paraiso.domain.votes.Vote
import com.paraiso.server.plugins.jobs.HomeJobs
import com.paraiso.server.plugins.jobs.ProfileJobs
import com.paraiso.server.plugins.jobs.SportJobs
import com.paraiso.server.util.cleanAndType
import com.paraiso.server.util.determineMessageType
import com.paraiso.server.util.getMentions
import com.paraiso.server.util.sendTypedMessage
import com.paraiso.server.util.validateUserName
import io.klogging.Klogging
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

class WebSocketHandler(
    private val serverId: String,
    private val userSessions: ConcurrentHashMap<String, ConcurrentHashMap<String, SessionContext>>,
    private val services: AppServices
) : Klogging {

    suspend fun connect(
        session: WebSocketServerSession,
        sessionContext: SessionContext,
        currentUser: User,
        isNewUser: Boolean
    ) {
        // Track this session in the local set
        val sessionsForUser = userSessions.computeIfAbsent(currentUser.id) { ConcurrentHashMap() }
        sessionsForUser[sessionContext.sessionId] = sessionContext
        session.handleUser(sessionContext.sessionId, currentUser, isNewUser, sessionContext)
    }

    private suspend fun WebSocketServerSession.handleUser(
        sessionId: String,
        currentUser: User,
        isNewUser: Boolean,
        sessionContext: SessionContext
    ) {
        // new user so create new route entry
        launch {
            if (isNewUser) {
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
                            pinnedPostIds = emptyList(),
                            pinnedPosts = emptyMap(),
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
                services.usersApi.saveUser(currentUser)
            }
        }
        // create or update session connected status
        launch {
            val sessionToSave = services.cacheService.getUserSession(currentUser.id)?.let { existingSession ->
                val serverSessions = existingSession.serverSessions.toMutableMap()
                serverSessions[serverId] = (serverSessions[serverId] ?: emptySet()) + sessionId
                existingSession.copy(
                    serverSessions = serverSessions
                )
            } ?: run {
                UserSession(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.id,
                    serverSessions = mapOf(serverId to setOf(sessionId)),
                    status = UserStatus.CONNECTED
                )
            }
            services.cacheService.saveUserSession(sessionToSave)
        }

        joinChat(currentUser, sessionId, sessionContext)
    }

    private suspend fun validateMessage(
        sessionUserId: String,
        blocking: Boolean,
        message: Message,
        sessionContext: SessionContext
    ) = message.userId?.let { userId -> // user id not null condition
        val isUser = sessionUserId == userId
        // user is on home - take any message, otherwise message must come from route
        val routeCriteria = sessionContext.routeId == HOME_PREFIX || sessionContext.routeId == message.route
        val filterCriteria =
            sessionContext.filterTypes.userRoles.contains(message.userRole) &&
                sessionContext.filterTypes.postTypes.contains(message.type) &&
                routeCriteria
        val messageCriteria = !blocking && filterCriteria // user isn't in cur user's block list
        isUser || messageCriteria
    } ?: false

    private suspend fun WebSocketServerSession.joinChat(
        incomingUser: User,
        sessionId: String,
        sessionContext: SessionContext
    ) {
        //user may change when banned or when user updates settings (but only care about id, banned, and roles)
        var sessionUser = incomingUser.copy()
        sendTypedMessage(MessageType.USER, sessionUser)

        // setup message intake (from others)
        launch {
            ServerState.eventReceivedFlow.collect { event ->
                when (event) {
                    is ServerEvent.MessageReceived -> {
                        event.data.let { message ->
                            val block = services.blocksApi.findIn(
                                sessionUser.id,
                                listOf(message.userId ?: "")
                            ).firstOrNull()
                            if (
                                validateMessage(
                                    sessionUser.id,
                                    block?.blocking == true,
                                    message,
                                    sessionContext
                                )
                            ) {
                                val mappedMessage = if (sessionContext.routeId == HOME_PREFIX && message.replyId == message.route) {
                                    message.copy(replyId = HOME_PREFIX)
                                } else {
                                    message
                                }
                                sendTypedMessage(MessageType.MSG, mappedMessage)
                            }
                        }
                    }
                    is ServerEvent.VoteReceived -> {
                        event.data.let { newVote ->
                            if (sessionContext.filterTypes.postTypes.contains(newVote.type)) {
                                sendTypedMessage(MessageType.VOTE, newVote)
                            }
                        }
                    }
                    is ServerEvent.FollowReceived -> sendTypedMessage(MessageType.FOLLOW, event.data)
                    is ServerEvent.FavoriteReceived -> sendTypedMessage(MessageType.FAVORITE, event.data)
                    is ServerEvent.DeleteReceived -> sendTypedMessage(MessageType.DELETE, event.data)
                    is ServerEvent.UserUpdateReceived -> {
                        val user = event.data
                        // remove private data from user update socket messages
                        val finalizedUser = if(user.id == sessionUser.id){
                            user
                        } else {
                            // grab session user's context for user that changed
                            val follow = services.followsApi.findIn(sessionUser.id, listOf(user.id)).firstOrNull()
                            val block = services.blocksApi.findIn(sessionUser.id, listOf(user.id)).firstOrNull()
                            val userWithData = user.copy(
                                viewerContext = ViewerContext(
                                    following = follow?.following,
                                    blocking = block?.blocking
                                )
                            )
                            userWithData.toBasicResponse()
                        }
                        sendTypedMessage(
                            MessageType.USER_UPDATE,
                            finalizedUser
                        )
                    }
                    is ServerEvent.RouteUpdateReceived -> {
                        val route = event.data
                        // remove private data from user update socket messages
                        if(sessionContext.routeId == route.id){
                            sendTypedMessage(
                                MessageType.ROUTE_UPDATE,
                                route
                            )
                        }
                    }
                    is ServerEvent.PostPinReceived -> sendTypedMessage(MessageType.PIN_POST, event.data)
                    is ServerEvent.RoleUpdateReceived -> sendTypedMessage(MessageType.ROLE_UPDATE, event.data)
                    is ServerEvent.BanReceived -> {
                        event.data.let { bannedMsg ->
                            if (sessionUser.id == bannedMsg.userId) {
                                sessionUser = sessionUser.copy(banned = true)
                            }
                        }
                    }
                    is ServerEvent.TagReceived -> sendTypedMessage(MessageType.TAG, event.data)
                    is ServerEvent.UserReportReceived -> sendTypedMessage(MessageType.REPORT_USER, event.data)
                    is ServerEvent.PostReportReceived -> sendTypedMessage(MessageType.REPORT_POST, event.data)
                    is ServerEvent.BasicReceived -> sendTypedMessage(MessageType.BASIC, event.data)
                    else -> logger.error { "Found unknown type when sending typed message from flow" }
                }
            }
        }

        ServerState.eventReceivedFlowMut.emit(ServerEvent.UserUpdateReceived(sessionUser.toBasicResponse()))
        services.eventService.publish(MessageType.USER_UPDATE.name, "$serverId:${Json.encodeToString(sessionUser)}")
        // holds the active jobs for given route
        val activeJobs = mutableListOf<Job>()
        val activeComps = ConcurrentHashMap<Set<String>, Job>()
        val activeBoxScores = ConcurrentHashMap<String, Job>()
        try {
            // Continuously collect messages pushed from the Redis listener
            launch {
                try {
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
            // Wait 23 hours before bumping the user session (refresh ttl of key)
            launch {
                while (isActive) {
                    delay(23.hours)
                    val extended = services.cacheService.bumpUserSession(sessionUser.id) ?: false
                    if (!extended) {
                        break
                    }
                }
            }
            //consume and parse incoming messages
            for (frame in incoming) {
                when (val messageType = determineMessageType(frame)) {
                    MessageType.MSG -> {
                        converter?.cleanAndType<Message>(frame)?.let { message ->
                            handleMessage(message, sessionUser.id, sessionUser.banned)
                        }
                    }
                    MessageType.DM -> {
                        converter?.cleanAndType<DirectMessage>(frame)?.let { dm ->
                            handleDm(dm, sessionUser.id, sessionUser.banned)
                        }
                    }
                    MessageType.FOLLOW -> {
                        converter?.cleanAndType<Follow>(frame)?.copy(followerId = sessionUser.id)?.let { follow ->
                            if (sessionUser.banned) {
                                sendTypedMessage(MessageType.FOLLOW, follow)
                            } else {
                                launch { services.followsApi.follow(follow) }
                                launch { services.usersApi.follow(follow) }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.FollowReceived(follow))
                                services.eventService.publish(MessageType.FOLLOW.name, "$serverId:${Json.encodeToString(follow)}")
                            }
                        }
                    }
                    MessageType.FAVORITE -> {
                        converter?.cleanAndType<Favorite>(frame)?.copy(userId = sessionUser.id)?.let { favorite ->
                            if (sessionUser.banned) {
                                sendTypedMessage(MessageType.FAVORITE, favorite)
                            } else {
                                launch { services.usersApi.toggleFavoriteRoute(favorite) }
                                // indicates toggle on or off rather than adding route flair
                                if (favorite.toggle) {
                                    launch { services.routesApi.toggleFavoriteRoute(favorite) }
                                }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.FavoriteReceived(favorite))
                                services.eventService.publish(MessageType.FAVORITE.name, "$serverId:${Json.encodeToString(favorite)}")
                            }
                        }
                    }
                    MessageType.VOTE -> {
                        converter?.cleanAndType<Vote>(frame)?.copy(voterId = sessionUser.id)?.let { vote ->
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
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.VoteReceived(vote.copy(score = score)))
                                services.eventService.publish(MessageType.VOTE.name, "$serverId:${Json.encodeToString(vote)}")
                            }
                        }
                    }
                    MessageType.USER_UPDATE -> {
                        converter?.cleanAndType<User>(frame)?.copy(id = sessionUser.id)?.let { user ->
                            if (user.validateUserName()) {
                                launch { services.usersApi.updateUser(user) }
                                launch {
                                    if(sessionUser.name != user.name){
                                        user.name?.let { name ->
                                            val routeId = "/p/${user.id}"
                                            services.routesApi.setRouteTitle(routeId, name)
                                            ServerState.eventReceivedFlowMut.emit(
                                                ServerEvent.RouteUpdateReceived(
                                                    RouteUpdate(routeId, name)
                                                )
                                            )
                                        }
                                    }
                                }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.UserUpdateReceived(user.toBasicResponse()))
                                sendTypedMessage(MessageType.USER, user) // send full user details back to user
                                services.eventService.publish(MessageType.USER_UPDATE.name, "$serverId:${Json.encodeToString(user)}")
                            }
                        }
                    }
                    MessageType.FILTER_TYPES -> {
                        converter?.cleanAndType<FilterTypes>(frame)?.let { newFilterTypes ->
                            sessionContext.filterTypes = newFilterTypes
                        }
                    }
                    MessageType.DELETE -> {
                        converter?.cleanAndType<Delete>(frame)?.let { delete ->
                            // validate post is owned by user and delete is executed
                            services.postsApi.deletePost(delete, sessionUser.id)?.let { deleted ->
                                if( deleted ){
                                    ServerState.eventReceivedFlowMut.emit(ServerEvent.DeleteReceived(delete))
                                    services.eventService.publish(MessageType.DELETE.name, "$serverId:${Json.encodeToString(delete)}")
                                }
                            }
                        }
                    }
                    MessageType.PIN_POST -> {
                        converter?.cleanAndType<PostPin>(frame)?.copy(userId = sessionUser.id)?.let { postPin ->
                            // user must have elevated auth or be pinning post on profile
                            val routeUserId = postPin.routeId.removePrefix(USER_PREFIX)
                            if (sessionUser.roles == UserRole.ADMIN || sessionUser.roles == UserRole.MOD || (sessionUser.id == routeUserId)) {
                                launch {
                                    if(postPin.order == -1){
                                        postPin.id?.let{
                                            services.postPinsApi.delete(it)
                                        }
                                    }else{
                                        services.postPinsApi.save(postPin)
                                    }
                                }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.PostPinReceived(postPin))
                                services.eventService.publish(MessageType.PIN_POST.name, "$serverId:${Json.encodeToString(postPin)}")
                            }
                        }
                    }
                    MessageType.ROLE_UPDATE -> {
                        converter?.cleanAndType<RoleUpdate>(frame)?.let { roleUpdate ->
                            if (sessionUser.roles == UserRole.ADMIN || (sessionUser.roles == UserRole.MOD && roleUpdate.userRole != UserRole.ADMIN)) {
                                launch { services.usersApi.setUserRole(roleUpdate) }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.RoleUpdateReceived(roleUpdate))
                                services.eventService.publish(MessageType.ROLE_UPDATE.name, "$serverId:${Json.encodeToString(roleUpdate)}")
                            }
                        }
                    }
                    MessageType.BAN -> {
                        converter?.cleanAndType<Ban>(frame)?.let { ban ->
                            if (sessionUser.roles == UserRole.ADMIN) {
                                launch { services.usersApi.banUser(ban) }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.BanReceived(ban))
                                services.eventService.publish(MessageType.BAN.name, "$serverId:${Json.encodeToString(ban)}")
                            }
                        }
                    }
                    MessageType.TAG -> {
                        converter?.cleanAndType<Tag>(frame)?.let { tag ->
                            if (sessionUser.roles == UserRole.ADMIN) {
                                launch { services.usersApi.tagUser(tag) }
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.TagReceived(tag))
                                services.eventService.publish(MessageType.TAG.name, "$serverId:${Json.encodeToString(tag)}")
                            }
                        }
                    }
                    MessageType.REPORT_USER -> {
                        converter?.cleanAndType<Report>(frame)?.let { reportUser ->
                            launch { services.adminApi.reportUser(sessionUser.id, reportUser) }
                            launch { services.usersApi.addReport() }
                            ServerState.eventReceivedFlowMut.emit(ServerEvent.UserReportReceived(reportUser))
                            services.eventService.publish(MessageType.REPORT_USER.name, "$serverId:${Json.encodeToString(reportUser)}")
                        }
                    }
                    MessageType.REPORT_POST -> {
                        converter?.cleanAndType<Report>(frame)?.let { reportPost ->
                            launch { services.adminApi.reportPost(sessionUser.id, reportPost) }
                            launch { services.usersApi.addReport() }
                            ServerState.eventReceivedFlowMut.emit(ServerEvent.PostReportReceived(reportPost))
                            services.eventService.publish(MessageType.REPORT_POST.name, "$serverId:${Json.encodeToString(reportPost)}")
                        }
                    }
                    MessageType.ROUTE -> {
                        converter?.cleanAndType<Route>(frame)?.let { route ->
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
                        converter?.cleanAndType<Subscription>(frame)?.let { subscription ->
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
                services.cacheService.getUserSession(sessionUser.id)?.let { existingSession ->
                    val serverSessions = existingSession.serverSessions.toMutableMap()
                    // Remove this sessionId from the current server’s set
                    val remainingSessions = (serverSessions[serverId] ?: emptySet()) - sessionId
                    if (remainingSessions.isEmpty()) {
                        serverSessions.remove(serverId)
                    } else {
                        serverSessions[serverId] = remainingSessions
                    }
                    val userDisconnected = services.userSessionsApi.getUserById(sessionUser.id, sessionUser.id, false)
                        ?.copy(status = UserStatus.DISCONNECTED)
                    if (serverSessions.isEmpty()) {
                        // Fully disconnected, remove from redis and publish out user disconnect
                        services.cacheService.deleteUserSession(sessionUser.id)
                        if (userDisconnected != null) {
                            ServerState.eventReceivedFlowMut.emit(ServerEvent.UserUpdateReceived(userDisconnected))
                            services.eventService.publish(
                                MessageType.USER_UPDATE.name,
                                "$serverId:${Json.encodeToString(userDisconnected)}"
                            )
                        }
                    } else {
                        // Not fully disconnected so just update redis session list
                        services.cacheService.saveUserSession(existingSession.copy(serverSessions = serverSessions))
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
        userId: String,
        userBanned: Boolean
    ) = coroutineScope {
        val messageId: String = message.editId ?: UUID.randomUUID().toString()
        val userIdMentions = services.usersApi.addMentions(
            // retreive mentions from content (parse with @)
            getMentions(message.content),
            message.userReceiveIds.firstOrNull(),
            userId
        )
        // create notifications for all mentions or replied users
        userIdMentions.map { userReceiveId ->
            val type = if (message.userReceiveIds.firstOrNull() == userReceiveId) {
                NotificationType.POST
            } else {
                NotificationType.MENTION
            }
            Notification(
                id = "$userReceiveId-${userId}-$messageId-${message.replyId}",
                userId = userReceiveId,
                createUserId = userId,
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
                Vote(
                    voterId = userId,
                    voteeId = userId,
                    type = message.type,
                    postId = messageId,
                    upvote = true
                )
            )
        }
        // add init vote to user score
        launch {
            services.usersApi.votePost(
                userId,
                1
            )
        }
        message.copy(
            id = messageId,
            userId = userId,
            // if root id == message id then it's a post at the root of the route
            rootId = messageId.takeIf { message.rootId == null } ?: message.rootId,
            userReceiveIds = message.userReceiveIds.plus(userIdMentions)
        ).let { messageWithData ->
            // shadow send message if user banned
            if (userBanned) {
                sendTypedMessage(MessageType.MSG, messageWithData)
            } else {
                // emit to this server, publish downstream to other servers
                launch { services.postsApi.putPost(messageWithData) }
                ServerState.eventReceivedFlowMut.emit(ServerEvent.MessageReceived(messageWithData))
                services.eventService.publish(MessageType.MSG.name, "$serverId:${Json.encodeToString(messageWithData)}")
            }
        }
    }
    private suspend fun WebSocketServerSession.handleDm(
        dm: DirectMessage,
        userId: String,
        userBanned: Boolean
    ) = coroutineScope {
        dm.copy(
            id = UUID.randomUUID().toString(),
            userId = userId
        ).let { dmWithData ->
            launch { sendTypedMessage(MessageType.DM, dmWithData) }
            val userReceiveBlocking = services.blocksApi.findIn(
                dmWithData.userReceiveId,
                listOf(userId)
            ).firstOrNull()?.blocking == true
            if (
                !userBanned &&
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
                    services.cacheService.getUserSession(dmWithData.userReceiveId)?.let { receiveUserSessions ->
                        val dmString = Json.encodeToString(dmWithData)
                        receiveUserSessions.serverSessions.keys
                            .filter { it != serverId }
                            .forEach { server ->
                                services.eventService.publish(
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
            SiteRoute.FAVORITES -> HomeJobs().homeJobs(session)
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

package com.paraiso

import com.auth0.jwk.JwkProviderBuilder
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.paraiso.api.admin.adminController
import com.paraiso.api.auth.authController
import com.paraiso.api.blocks.blocksController
import com.paraiso.api.follows.followsController
import com.paraiso.api.metadata.metadataController
import com.paraiso.api.notifications.notificationsController
import com.paraiso.api.posts.postsController
import com.paraiso.api.routes.routesController
import com.paraiso.api.sports.dataGenerationController
import com.paraiso.api.sports.sportController
import com.paraiso.api.userchats.directMessagesController
import com.paraiso.api.userchats.userChatsController
import com.paraiso.api.users.userSessionsController
import com.paraiso.api.users.usersController
import com.paraiso.api.util.UserCookie
import com.paraiso.cache.CacheServiceImpl
import com.paraiso.client.metadata.MetadataClientImpl
import com.paraiso.client.sport.SportClientImpl
import com.paraiso.database.admin.PostReportsDBImpl
import com.paraiso.database.admin.UserReportsDBImpl
import com.paraiso.database.blocks.BlocksDBImpl
import com.paraiso.database.follows.FollowsDBImpl
import com.paraiso.database.notifications.NotificationsDBImpl
import com.paraiso.database.posts.PostPinsDBImpl
import com.paraiso.database.posts.PostsDBImpl
import com.paraiso.database.routes.RoutesDBImpl
import com.paraiso.database.sports.AthletesDBImpl
import com.paraiso.database.sports.BoxscoresDBImpl
import com.paraiso.database.sports.CoachesDBImpl
import com.paraiso.database.sports.CompetitionsDBImpl
import com.paraiso.database.sports.LeadersDBImpl
import com.paraiso.database.sports.LeaguesDBImpl
import com.paraiso.database.sports.PlayoffsDBImpl
import com.paraiso.database.sports.RostersDBImpl
import com.paraiso.database.sports.SchedulesDBImpl
import com.paraiso.database.sports.ScoreboardsDBImpl
import com.paraiso.database.sports.StandingsDBImpl
import com.paraiso.database.sports.TeamsDBImpl
import com.paraiso.database.userchats.DirectMessagesDBImpl
import com.paraiso.database.userchats.UserChatsDBImpl
import com.paraiso.database.users.UsersDBImpl
import com.paraiso.database.votes.VotesDBImpl
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.blocks.BlocksApi
import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.notifications.NotificationsApi
import com.paraiso.domain.posts.PostPinsApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.sport.sports.SportDBs
import com.paraiso.domain.sport.sports.SportHandler
import com.paraiso.domain.userchats.DirectMessagesApi
import com.paraiso.domain.userchats.UserChatsApi
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.users.newUser
import com.paraiso.domain.users.systemUser
import com.paraiso.domain.util.Constants.MAIN_SERVER
import com.paraiso.domain.util.ServerConfig.autoBuild
import com.paraiso.domain.votes.VotesApi
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.plugins.MessageHandler
import com.paraiso.server.plugins.ServerHandler
import com.paraiso.server.plugins.SessionContext
import com.paraiso.server.plugins.WebSocketHandler
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.util.AttributeKey
import io.ktor.util.hex
import io.lettuce.core.RedisClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

fun main() {
    val job = SupervisorJob()
    val jobScope = CoroutineScope(Dispatchers.Default + job)
    val server = embeddedServer(Netty, port = 8080) {
        module(jobScope)
    }.start(wait = true)
    // cancel all coroutines on shutdown
    Runtime.getRuntime().addShutdownHook(
        Thread {
            try {
                println("Shutting down server...")
                runBlocking {
                    job.cancelAndJoin() // Cancel background jobs
                }
                server.stop(1000, 2000)
            } catch (ex: Exception) {
                // Log the exception instead of letting it bubble up
                println("Exception during shutdown: ${ex.message}")
                ex.printStackTrace()
            }
        }
    )
}

fun Application.module(jobScope: CoroutineScope) {
    // load config
    val config = HoconApplicationConfig(ConfigFactory.load().withFallback(ConfigFactory.systemEnvironment()).resolve())
    val serverId = config.property("server.id").getString()
    val mongoUrl = config.property("mongodb.url").getString()
    val mongoDB = config.property("mongodb.database").getString()
    val redisUrl = config.property("redis.url").getString()
    // setup DB
    val database = MongoClient.create(mongoUrl).getDatabase(mongoDB)
    val sportsDBs = SportDBs(
        LeaguesDBImpl(database),
        StandingsDBImpl(database),
        TeamsDBImpl(database),
        RostersDBImpl(database),
        AthletesDBImpl(database),
        CoachesDBImpl(database),
        SchedulesDBImpl(database),
        ScoreboardsDBImpl(database),
        BoxscoresDBImpl(database),
        CompetitionsDBImpl(database),
        LeadersDBImpl(database),
        PlayoffsDBImpl(database)
    )
    val postsDb = PostsDBImpl(database)
    val usersDb = UsersDBImpl(database)
    // setup redis
    val userSessions = ConcurrentHashMap<String, ConcurrentHashMap<String, SessionContext>>()
    val redisClient = RedisClient.create(redisUrl)
    val eventServiceImpl = EventServiceImpl(redisClient)
    val cacheServiceImpl = CacheServiceImpl(redisClient)
    // subscriber to all incoming messages from other servers
    val messageHandler = MessageHandler(serverId, userSessions, eventServiceImpl)
    jobScope.launch {
        messageHandler.messageJobs()
    }
    // setup apis and scopes
    val sportApi = SportApi(sportsDBs)
    val usersApi = UsersApi(usersDb)
    val authApi = AuthApi(usersApi, cacheServiceImpl)
    val votesApi = VotesApi(VotesDBImpl(database))
    val followsApi = FollowsApi(FollowsDBImpl(database))
    val blocksApi = BlocksApi(BlocksDBImpl(database))
    val postsApi = PostsApi(
        postsDb,
        votesApi,
        followsApi,
        usersApi,
        cacheServiceImpl,
        eventServiceImpl,
        sportApi
    )
    val postPinsApi = PostPinsApi(PostPinsDBImpl(database))
    val notificationsApi = NotificationsApi(NotificationsDBImpl(database), postsApi)
    val userSessionsApi = UserSessionsApi(usersDb, cacheServiceImpl, followsApi, blocksApi)
    val routesApi = RoutesApi(RoutesDBImpl(database), postsApi, postPinsApi, userSessionsApi)
    val directMessagesApi = DirectMessagesApi(DirectMessagesDBImpl(database))
    val userChatsApi = UserChatsApi(UserChatsDBImpl(database), directMessagesApi)
    val adminApi = AdminApi(PostReportsDBImpl(database), UserReportsDBImpl(database), usersApi)
    val metadataApi = MetadataApi(MetadataClientImpl())
    val services = AppServices(
        authApi,
        adminApi,
        postsApi,
        postPinsApi,
        routesApi,
        usersApi,
        votesApi,
        followsApi,
        blocksApi,
        notificationsApi,
        userSessionsApi,
        userChatsApi,
        directMessagesApi,
        sportApi,
        metadataApi,
        cacheServiceImpl,
        eventServiceImpl
    )
    // build handlers early - for data generation
    val sportHandler = SportHandler(
        SportClientImpl(),
        routesApi,
        sportsDBs,
        eventServiceImpl,
        postsDb
    )
    val serverHandler = ServerHandler(routesApi)
    // only launch data fetching jobs on a single server - will split off to microservice
    if (serverId == MAIN_SERVER) {
        jobScope.launch {
            sportHandler.bootJobs(SiteRoute.FOOTBALL)
        }
        jobScope.launch {
            sportHandler.bootJobs(SiteRoute.BASKETBALL)
        }
        jobScope.launch {
            sportHandler.bootJobs(SiteRoute.HOCKEY)
        }
        jobScope.launch {
            serverHandler.bootJobs()
        }
    }
    // build handler and configure sockets
    val handler = WebSocketHandler(
        serverId = serverId,
        userSessions,
        services
    )
    if (autoBuild) {
        runBlocking {
            usersApi.saveUser(User.systemUser())
        }
    }
    configureSockets(config, handler, services, serverHandler, sportHandler)
    // close subscription to redis
    monitor.subscribe(ApplicationStopped) {
        eventServiceImpl.close()
    }
}

fun Application.configureSockets(
    config: HoconApplicationConfig,
    handler: WebSocketHandler,
    services: AppServices,
    serverHandler: ServerHandler,
    sportHandler: SportHandler
) {
    configureFeatures(config)
    configureSecurity(config)
    val userKey = AttributeKey<User>("UserKey")
    val isNewKey = AttributeKey<Boolean>("IsNewKey")
    routing {
        route("/chat") {
            intercept(ApplicationCallPipeline.Plugins) {
                // grab userId and roles from signed cookies - need role to validate REST requests
                val userCookie = call.sessions.get<UserCookie>()
                // use ticket to grab authenticated user, fallback to guest account
                val ticketedUserId = call.request.queryParameters["ticket"]?.let { services.cacheService.redeemTicket(it) }
                val resolvedUserId = ticketedUserId ?: userCookie?.userId
                // check if user already exists based on user or passed in id
                val existingUser = resolvedUserId?.let{
                    //need to fetch full user info
                    services.userSessionsApi.getUserById(it, null, true)
                }
                val (currentUser, isNewUser) = if(existingUser == null){
                    User.newUser(UUID.randomUUID().toString()) to true
                } else {
                    existingUser to false
                }
                // set the final resolved user id as a signed cookie for the user
                call.sessions.set(
                    UserCookie(
                        userId = currentUser.id,
                        role = currentUser.roles
                    )
                )
                // Store the resolved user in the call attributes so the WS can see it
                call.attributes.put(userKey, currentUser)
                call.attributes.put(isNewKey, isNewUser)
                proceed()
            }
            webSocket {
                val sessionContext = SessionContext(this)
                val currentUser = call.attributes[userKey].copy(
                    status = UserStatus.CONNECTED,
                    sessionId = sessionContext.sessionId // session id used to tie event data to user's tab
                )
                val isNewUser = call.attributes[isNewKey]
                handler.connect(
                    this,
                    sessionContext,
                    currentUser,
                    isNewUser
                )
            }
        }
        route("paraiso_api/v1") {
            authController(services.authApi, config)
            postsController(services.postsApi)
            usersController(services.usersApi)
            userSessionsController(services.userSessionsApi)
            userChatsController(services.userChatsApi)
            directMessagesController(services.directMessagesApi)
            sportController(services.sportApi)
            metadataController(services.metadataApi)
            adminController(services.adminApi)
            routesController(services.routesApi)
            followsController(services.followsApi)
            blocksController(services.blocksApi)
            notificationsController(services.notificationsApi)
            dataGenerationController(serverHandler, sportHandler)
        }
    }
}
fun Application.configureFeatures(config: HoconApplicationConfig) {
    val mainHost = config.property("api.mainHost").getString()
    val altHost = config.property("api.altHost").getString()
    val frontendHost = config.property("api.frontendHost").getString()
    val frontendPreviewHost = config.property("api.frontendPreviewHost").getString()
    val backendHost = config.property("api.backendHost").getString()
    val scheme = config.property("api.scheme").getString()
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = 30.seconds
        timeout = 45.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CORS) {
        allowHost(mainHost, schemes = listOf(scheme))
        allowHost(altHost, schemes = listOf(scheme))
        allowHost(frontendHost, schemes = listOf(scheme))
        allowHost(frontendPreviewHost, schemes = listOf(scheme))
        allowHost(backendHost, schemes = listOf(scheme))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }
    // install signed cookies
    val secretEncryptKeyString = config.property("api.secretEncryptKey").getString()
    val secretSignKeyString = config.property("api.secretSignKey").getString()
    install(Sessions) {
        cookie<UserCookie>("USER_SESSION") {
            val secretEncryptKey = hex(secretEncryptKeyString) // Use a real key from ENV
            val secretSignKey = hex(secretSignKeyString)    // Use a real key from ENV

            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
            cookie.path = "/"
            cookie.httpOnly = true  // JS cannot touch this
            cookie.secure = true    // Only sent over HTTPS
            cookie.extensions["SameSite"] = "Strict"
        }
    }
}

fun Application.configureSecurity(config: HoconApplicationConfig) {
    authentication {
        jwt("auth0") {
            realm = "ekoes-io"
            val authDomain = config.property("auth.auth0Domain").getString()
            val authAudience = config.property("auth.auth0Audience").getString()
            val jwkProvider = JwkProviderBuilder(authDomain)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()
            verifier(jwkProvider, "https://$authDomain/"){
                acceptLeeway(30)
            }
            validate { credential ->
                if (credential.payload.audience.contains(authAudience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                val failures = call.authentication.allFailures
                println("Auth Validation Failed: ${failures.joinToString()}")
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}

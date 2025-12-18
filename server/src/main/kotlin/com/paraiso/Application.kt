package com.paraiso

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.paraiso.api.admin.adminController
import com.paraiso.api.auth.authController
import com.paraiso.api.blocks.blocksController
import com.paraiso.api.follows.followsController
import com.paraiso.api.metadata.metadataController
import com.paraiso.api.notifications.notificationsController
import com.paraiso.api.posts.postPinsController
import com.paraiso.api.posts.postsController
import com.paraiso.api.routes.routesController
import com.paraiso.api.sports.dataGenerationController
import com.paraiso.api.sports.sportController
import com.paraiso.api.userchats.directMessagesController
import com.paraiso.api.userchats.userChatsController
import com.paraiso.api.users.userSessionsController
import com.paraiso.api.users.usersController
import com.paraiso.api.votes.votesController
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
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
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
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.lettuce.core.RedisClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

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
        LeadersDBImpl(database)
    )
    val postsDb = PostsDBImpl(database)
    val usersDb = UsersDBImpl(database)
    // setup redis
    val userSessions = ConcurrentHashMap<String, ConcurrentHashMap<String, SessionContext>>()
    val redisClient = RedisClient.create(redisUrl)
    val eventServiceImpl = EventServiceImpl(redisClient)
    // subscriber to all incoming messages from other servers
    val messageHandler = MessageHandler(serverId, userSessions, eventServiceImpl)
    jobScope.launch {
        messageHandler.messageJobs()
    }
    // setup apis and scopes
    val authApi = AuthApi()
    val sportApi = SportApi(sportsDBs)
    val usersApi = UsersApi(usersDb)
    val votesApi = VotesApi(VotesDBImpl(database))
    val followsApi = FollowsApi(FollowsDBImpl(database))
    val blocksApi = BlocksApi(BlocksDBImpl(database))
    val postsApi = PostsApi(
        postsDb,
        votesApi,
        followsApi,
        eventServiceImpl,
        sportApi
    )
    val postPinsApi = PostPinsApi(PostPinsDBImpl(database))
    val notificationsApi = NotificationsApi(NotificationsDBImpl(database), postsApi)
    val userSessionsApi = UserSessionsApi(usersDb, eventServiceImpl, followsApi, blocksApi)
    val routesApi = RoutesApi(RoutesDBImpl(database), postsApi, postPinsApi, userSessionsApi)
    val directMessagesApi = DirectMessagesApi(DirectMessagesDBImpl(database))
    val userChatsApi = UserChatsApi(UserChatsDBImpl(database), directMessagesApi)
    val adminApi = AdminApi(PostReportsDBImpl(database), UserReportsDBImpl(database))
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
        metadataApi
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
        eventServiceImpl,
        userSessions,
        services
    )
    if (autoBuild) {
        runBlocking {
            usersApi.saveUser(UserResponse.systemUser())
        }
    }
    configureSockets(config, handler, services, serverHandler, sportHandler)
    // close subscription to redis
    environment.monitor.subscribe(ApplicationStopped) {
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
    routing {
        webSocket("chat") {
            handler.connect(this)
        }
        route("paraiso_api/v1") {
            authController(services.authApi)
            postsController(services.postsApi)
            postPinsController(services.postPinsApi)
            usersController(services.usersApi)
            userSessionsController(services.userSessionsApi)
            userChatsController(services.userChatsApi)
            directMessagesController(services.directMessagesApi)
            sportController(services.sportApi)
            metadataController(services.metadataApi)
            adminController(services.adminApi)
            routesController(services.routesApi)
            votesController(services.votesApi)
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
    val backendHost = config.property("api.backendHost").getString()
    val scheme = config.property("api.scheme").getString()
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(20)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CORS) {
        allowHost(mainHost, schemes = listOf(scheme))
        allowHost(altHost, schemes = listOf(scheme))
        allowHost(frontendHost, schemes = listOf(scheme))
        allowHost(backendHost, schemes = listOf(scheme))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
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
}

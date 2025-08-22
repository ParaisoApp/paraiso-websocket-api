package com.paraiso

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.paraiso.client.sport.adapters.BBallOperationAdapter
import com.paraiso.client.sport.adapters.FBallOperationAdapter
import com.paraiso.com.paraiso.AppServices
import com.paraiso.com.paraiso.api.admin.adminController
import com.paraiso.com.paraiso.api.auth.authController
import com.paraiso.com.paraiso.api.metadata.metadataController
import com.paraiso.com.paraiso.api.posts.postsController
import com.paraiso.com.paraiso.api.routes.routesController
import com.paraiso.com.paraiso.api.sports.bball.bballController
import com.paraiso.com.paraiso.api.sports.fball.fballController
import com.paraiso.com.paraiso.api.users.userChatsController
import com.paraiso.com.paraiso.api.users.userSessionsController
import com.paraiso.com.paraiso.api.users.usersController
import com.paraiso.com.paraiso.server.plugins.ServerHandler
import com.paraiso.com.paraiso.server.plugins.jobs.MessageHandler
import com.paraiso.database.admin.PostReportsDBAdapterImpl
import com.paraiso.database.admin.UserReportsDBAdapterImpl
import com.paraiso.database.routes.RoutesDBAdapterImpl
import com.paraiso.database.sports.AthletesDBAdapterImpl
import com.paraiso.database.sports.BoxscoresDBAdapterImpl
import com.paraiso.database.sports.CoachesDBAdapterImpl
import com.paraiso.database.sports.CompetitionsDBAdapterImpl
import com.paraiso.database.sports.LeadersDBAdapterImpl
import com.paraiso.database.sports.RostersDBAdapterImpl
import com.paraiso.database.sports.SchedulesDBAdapterImpl
import com.paraiso.database.sports.ScoreboardsDBAdapterImpl
import com.paraiso.database.sports.StandingsDBAdapterImpl
import com.paraiso.database.sports.TeamsDBAdapterImpl
import com.paraiso.database.users.UserChatsDBAdapterImpl
import com.paraiso.database.users.UsersDBAdapterImpl
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.sport.sports.SportDBs
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.domain.sport.sports.bball.BBallHandler
import com.paraiso.domain.sport.sports.fball.FBallApi
import com.paraiso.domain.sport.sports.fball.FBallHandler
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.util.ServerState
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.plugins.WebSocketHandler
import com.paraiso.server.util.sendTypedMessage
import com.typesafe.config.ConfigFactory
import io.klogging.Klogging
import io.klogging.logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Duration
import io.lettuce.core.RedisClient
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.SerializationException
import java.util.concurrent.ConcurrentHashMap


fun main() {
    val job = SupervisorJob()
    val jobScope = CoroutineScope(Dispatchers.Default + job)

    val server = embeddedServer(Netty, port = 8080) {
        module(jobScope)
    }.start(wait = true)

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

fun Application.module(jobScope: CoroutineScope){
    //Application level logger
    val logger = logger("Application")
    //load config
    val config = HoconApplicationConfig(ConfigFactory.load())
    val serverId = config.property("server.id").getString()
    val mongoUrl = config.property("mongodb.url").getString()
    val mongoDB = config.property("mongodb.database").getString()
    val redisUrl = config.property("redis.url").getString()

    //setup DB
    val database = MongoClient.create(mongoUrl).getDatabase(mongoDB)
    val sportsDBs = SportDBs(
        StandingsDBAdapterImpl(database),
        TeamsDBAdapterImpl(database),
        RostersDBAdapterImpl(database),
        AthletesDBAdapterImpl(database),
        CoachesDBAdapterImpl(database),
        SchedulesDBAdapterImpl(database),
        ScoreboardsDBAdapterImpl(database),
        BoxscoresDBAdapterImpl(database),
        CompetitionsDBAdapterImpl(database),
        LeadersDBAdapterImpl(database)
    )
    val usersDb = UsersDBAdapterImpl(database)
    val userChatsDb = UserChatsDBAdapterImpl(database)
    val userReportsDb = UserReportsDBAdapterImpl(database)
    val postReportsDb = PostReportsDBAdapterImpl(database)

    //setup redis
    val userSessions = ConcurrentHashMap<String, Set<WebSocketServerSession>>()
    val redisClient = RedisClient.create(redisUrl)
    val eventServiceImpl = EventServiceImpl(serverId, redisClient)

    //subscriber to all incoming messages from other servers
    val messageHandler = MessageHandler(serverId, userSessions, eventServiceImpl)

    jobScope.launch {
        messageHandler.messageJobs()
    }

    //setup apis and scopes
    val routesApi = RoutesApi(RoutesDBAdapterImpl(database))
    jobScope.launch {
        BBallHandler(
            BBallOperationAdapter(),
            routesApi,
            sportsDBs
        ).bootJobs()
    }
    jobScope.launch {
        FBallHandler(
            FBallOperationAdapter(),
            routesApi,
            sportsDBs
        ).bootJobs()
    }
    jobScope.launch {
        ServerHandler(routesApi).bootJobs()
    }
    val authApi = AuthApi()
    val bballApi = BBallApi(sportsDBs)
    val fballApi = FBallApi(sportsDBs)
    val postsApi = PostsApi()
    val usersApi = UsersApi(usersDb)
    val userSessionsApi = UserSessionsApi(usersDb, eventServiceImpl)
    val userChatsApi = UserChatsApi(userChatsDb)
    val adminApi = AdminApi(postReportsDb, userReportsDb)
    val metadataApi = MetadataApi()

    val services = AppServices(
        authApi,
        adminApi,
        postsApi,
        routesApi,
        usersApi,
        userSessionsApi,
        userChatsApi,
        bballApi,
        fballApi,
        metadataApi
    )

    val handler = WebSocketHandler(
        serverId = serverId,
        eventServiceImpl,
        userSessions,
        services.usersApi,
        services.userChatsApi,
        services.postsApi,
        services.adminApi,
        services.routesApi,
        services.bBallApi,
        services.fBallApi
    )

    configureSockets(handler, services)

    environment.monitor.subscribe(ApplicationStopped) {
        eventServiceImpl.close()
    }
}
fun Application.configureFeatures() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(CORS) {
        anyHost() // 🚨 dev only
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

fun Application.configureSockets(handler: WebSocketHandler, services: AppServices) {
    configureFeatures()
    routing {
        webSocket("chat") {
            handler.handleUser(this)
        }
        route("paraiso_api/v1") {
            authController(services.authApi)
            postsController(services.postsApi)
            usersController(services.usersApi)
            userSessionsController(services.userSessionsApi)
            userChatsController(services.userChatsApi)
            bballController(services.bBallApi)
            fballController(services.fBallApi)
            metadataController(services.metadataApi)
            adminController(services.adminApi)
            routesController(services.routesApi)
        }
    }
}

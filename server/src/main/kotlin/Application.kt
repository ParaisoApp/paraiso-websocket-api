package com.paraiso

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.paraiso.client.sport.adapters.BBallOperationAdapter
import com.paraiso.client.sport.adapters.FBallOperationAdapter
import com.paraiso.com.paraiso.api.admin.adminController
import com.paraiso.com.paraiso.api.auth.authController
import com.paraiso.com.paraiso.api.metadata.metadataController
import com.paraiso.com.paraiso.api.posts.postsController
import com.paraiso.com.paraiso.api.routes.routesController
import com.paraiso.com.paraiso.api.sports.bball.bballController
import com.paraiso.com.paraiso.api.sports.fball.fballController
import com.paraiso.com.paraiso.api.users.userChatsController
import com.paraiso.com.paraiso.api.users.usersController
import com.paraiso.com.paraiso.server.plugins.ServerHandler
import com.paraiso.database.admin.PostReportsDBAdapterImpl
import com.paraiso.database.admin.UserReportsDBAdapterImpl
import com.paraiso.database.routes.RoutesDBAdapterImpl
import com.paraiso.database.sports.StandingsDBAdapterImpl
import com.paraiso.database.users.UserChatsDBAdapterImpl
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.domain.sport.sports.bball.BBallHandler
import com.paraiso.domain.sport.sports.fball.FBallApi
import com.paraiso.domain.sport.sports.fball.FBallHandler
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.server.plugins.WebSocketHandler
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Duration

fun main() {
    val job = Job()
    val jobScope = CoroutineScope(Dispatchers.Default + job)

    val uri = "mongodb://localhost:27017"
    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase("ekoes")

    val routesApi = RoutesApi(RoutesDBAdapterImpl(database))
    val standingsDBAdapterImpl = StandingsDBAdapterImpl(database)

    jobScope.launch {
        BBallHandler(
            BBallOperationAdapter(),
            routesApi,
            standingsDBAdapterImpl
        ).bootJobs()
    }
    jobScope.launch {
        FBallHandler(
            FBallOperationAdapter(),
            routesApi, 
            standingsDBAdapterImpl
        ).bootJobs()
    }
    jobScope.launch {
        ServerHandler(routesApi).bootJobs()
    }

    val postsApi = PostsApi()
    val usersApi = UsersApi()
    val userChatsApi = UserChatsApi(UserChatsDBAdapterImpl(database))
    val adminApi = AdminApi(PostReportsDBAdapterImpl(database), UserReportsDBAdapterImpl(database))
    val handler = WebSocketHandler(usersApi, userChatsApi, postsApi, adminApi, routesApi)

    val server = embeddedServer(Netty, port = 8080) {
        configureSockets(
            handler,
            adminApi,
            routesApi,
            postsApi,
            usersApi,
            userChatsApi,
            AuthApi(),
            BBallApi(standingsDBAdapterImpl),
            FBallApi(standingsDBAdapterImpl),
            MetadataApi()
        )
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("Shutting down server...")
            runBlocking {
                job.cancelAndJoin() // Cancel background jobs
            }
            server.stop(1000, 2000)
        }
    )
}

fun Application.configureSockets(
    handler: WebSocketHandler,
    adminApi: AdminApi,
    routesApi: RoutesApi,
    postsApi: PostsApi,
    usersApi: UsersApi,
    userChatsApi: UserChatsApi,
    authApi: AuthApi,
    bBallApi: BBallApi,
    fBallApi: FBallApi,
    metadataApi: MetadataApi
) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CORS) {
        // Replace with your frontend origin (e.g., localhost:3000)
        anyHost() // 🚨 Use only for development. In prod, specify exact host.

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
    routing {
        webSocket("chat") {
            handler.handleUser(this)
        }
        route("paraiso_api/v1") {
            authController(authApi)
            postsController(postsApi)
            usersController(usersApi)
            userChatsController(userChatsApi)
            bballController(bBallApi)
            fballController(fBallApi)
            metadataController(metadataApi)
            adminController(adminApi)
            routesController(routesApi)
        }
    }
}

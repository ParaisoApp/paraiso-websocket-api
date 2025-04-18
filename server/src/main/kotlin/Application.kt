package com.paraiso

import com.paraiso.client.sport.SportOperationAdapter
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.messageTypes.Login
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.sport.SportApi
import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UsersApi
import com.paraiso.server.plugins.WebSocketHandler
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration

fun main() {
    val jobScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val sportHandler = SportHandler(SportOperationAdapter())
    val postsApi = PostsApi()
    val authApi = AuthApi()
    val usersApi = UsersApi()
    val sportApi = SportApi()

    jobScope.launch {
        sportHandler.buildScoreboard()
    }
    jobScope.launch {
        sportHandler.getStandings()
    }
    jobScope.launch {
        sportHandler.getTeams()
    }
    jobScope.launch {
        sportHandler.getLeaders()
    }

    val handler = WebSocketHandler(usersApi, postsApi)

    val server = embeddedServer(Netty, port = 8080) {
        configureSockets(handler, authApi, postsApi, usersApi, sportApi)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("Shutting down server...")
            jobScope.cancel() // Cancel background jobs
            server.stop(1000, 2000)
        }
    )
}

fun Application.configureSockets(
    handler: WebSocketHandler,
    authApi: AuthApi,
    postsApi: PostsApi,
    usersApi: UsersApi,
    sportApi: SportApi
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
            // authController.handleAuth(this)
            route("auth") {
                post {
                    // fake auth controller
                    authApi.getAuth(call.receive<Login>())?.let { role ->
                        call.respond(HttpStatusCode.OK, role)
                    }
                }
            }
            route("posts") {
                get {
                    postsApi.getPosts(
                        call.request.queryParameters["id"] ?: "",
                        call.request.queryParameters["name"] ?: ""
                    ).let {
                        call.respond(HttpStatusCode.OK, it)
                    }
                }
            }
            route("users") {
                get {
                    call.respond(HttpStatusCode.OK, usersApi.getUserList())
                }
                post {
                    usersApi.setSettings(
                        call.request.queryParameters["id"] ?: "",
                        call.receive<UserSettings>()
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            route("sport") {
                get("/teams") {
                    call.respond(HttpStatusCode.OK, sportApi.getTeams())
                }
                get("/standings") {
                    sportApi.getStandings()?.let {
                        call.respond(HttpStatusCode.OK, it)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }
                get("/leaders") {
                    sportApi.getLeaders()?.let {
                        call.respond(HttpStatusCode.OK, it)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }
                get("/leader_cats") {
                    sportApi.getLeaderCategories()?.let {
                        call.respond(HttpStatusCode.OK, it)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }
                get("/team_roster") {
                    sportApi.getTeamRoster(call.request.queryParameters["teamId"] ?: "")?.let {
                        call.respond(HttpStatusCode.OK, it)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }
                get("/team_schedule") {
                    sportApi.getTeamSchedule(call.request.queryParameters["teamId"] ?: "")?.let {
                        call.respond(HttpStatusCode.OK, it)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }
            }
        }
    }
}

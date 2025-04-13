package com.paraiso

import com.paraiso.client.sport.SportOperationAdapter
import com.paraiso.com.paraiso.api.auth.AuthController
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.sport.SportHandler
import com.paraiso.domain.messageTypes.Login
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

    val handler = WebSocketHandler(sportHandler)

    val server = embeddedServer(Netty, port = 8080) {
        configureSockets(handler)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("Shutting down server...")
            jobScope.cancel() // Cancel background jobs
            server.stop(1000, 2000)
        }
    )
}

fun Application.configureSockets(handler: WebSocketHandler) {
    val authController = AuthController()
    val authApi = AuthApi()
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
        }
    }
}

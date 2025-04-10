package com.paraiso.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets(handler: WebSocketHandler) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        post("/login") {
//            val credentials = call.receive<LoginRequest>()
//            val user = authenticate(credentials.username, credentials.password)
//
//            if (user != null) {
//                // Issue a token (e.g., JWT) after successful authentication
//                val token = generateJwtToken(user)
//            } else {
//                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
//            }
            call.respond(HttpStatusCode.OK)
        }
        webSocket("chat") {
            handler.handleUser(this)
        }
    }
}

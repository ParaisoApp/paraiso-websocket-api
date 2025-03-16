package com.example.plugins

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets() {
    val serverScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Clean up resources when the application stops
    environment.monitor.subscribe(ApplicationStopped) {
        serverScope.cancel() // Ensure coroutines are canceled on shutdown
    }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val handler = WebSocketHandler()
        serverScope.launch {
            webSocket("chat") {
                handler.handleGuest(this)
            }
        }
        serverScope.launch {
            handler.buildScoreboard()
        }
        serverScope.launch {
            handler.getStandings()
        }
        serverScope.launch {
            handler.getTeams()
        }
    }
}

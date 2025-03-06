package com.example.plugins

import com.example.testRestClient.sport.SportOperationAdapter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.Collections
import java.util.LinkedHashMap


fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val allConnectedUsers: MutableMap<String, User> = Collections.synchronizedMap(LinkedHashMap())
        val handler = WebSocketHandler()
        val sportAdapter = SportOperationAdapter()
        webSocket("chat") {
            coroutineScope {
                val scoreBoard = sportAdapter.getSchedule()
                println(scoreBoard)
            }
            handler.handleGuest(this, allConnectedUsers)
        }
    }
}

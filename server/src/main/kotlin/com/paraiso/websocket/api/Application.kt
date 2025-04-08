package com.paraiso.websocket.api

import com.paraiso.websocket.api.plugins.SportHandler
import com.paraiso.websocket.api.plugins.WebSocketHandler
import com.paraiso.websocket.api.plugins.configureSockets
import com.paraiso.websocket.api.testRestClient.sport.SportOperationAdapter
import com.paraiso.websocket.api.testRestClient.util.ApiConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun main() {
    val jobScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val apiConfig = ApiConfig()
    val sportOperationAdapter = SportOperationAdapter(apiConfig)
    val sportHandler = SportHandler(sportOperationAdapter)

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

    val handler = WebSocketHandler(sportHandler, apiConfig)

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

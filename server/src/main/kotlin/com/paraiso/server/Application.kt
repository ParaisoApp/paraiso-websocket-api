package com.paraiso.server

import com.paraiso.client.sport.SportOperationAdapter
import com.paraiso.domain.sport.SportHandler
import com.paraiso.server.plugins.WebSocketHandler
import com.paraiso.server.plugins.configureSockets
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

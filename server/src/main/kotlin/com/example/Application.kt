package com.example

import com.example.plugins.*
import com.example.testRestClient.sport.SportOperationAdapter
import com.example.testRestClient.util.ApiConfig
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

    val handler = WebSocketHandler(sportHandler)

    val server = embeddedServer(Netty, port = 8080) {
        configureSockets(handler)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down server...")
        jobScope.cancel() // Cancel background jobs
        server.stop(1000, 2000)
    })
}

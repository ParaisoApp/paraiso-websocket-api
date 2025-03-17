package com.example

import com.example.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val handler = WebSocketHandler()
    val server = embeddedServer(Netty, port = 8080) {
        configureSockets(handler)
    }


    val jobScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    jobScope.launch {
        handler.buildScoreboard()
    }
    jobScope.launch {
        handler.getStandings()
    }
    jobScope.launch {
        handler.getTeams()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down server...")
        jobScope.cancel() // Cancel background jobs
    })

    server.start(wait = true)
}

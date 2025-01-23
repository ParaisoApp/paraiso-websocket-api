package com.example

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val client = HttpClient() {
        install(WebSockets)
    }
    runBlocking {
        client.webSocket(method = HttpMethod.Get, host = "localhost", port = 8080, path = "/chat") {
            val outputJob = launch { getAndDisplayOutput() }
            val inputJob = launch { takeAndSendInput() }

            inputJob.join()
            outputJob.cancelAndJoin()
        }
    }
    client.close()
}

suspend fun DefaultClientWebSocketSession.getAndDisplayOutput() {
    try {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                println(frame.readText())
            }
        }
    } catch (e: Exception) {
        println("Error occurred while receiving ${e.localizedMessage}")
        return
    }
}

suspend fun DefaultClientWebSocketSession.takeAndSendInput() {
    val name = readlnOrNull() ?: ""
    send(Frame.Text(name))
    while (true) {
        val message = readlnOrNull() ?: ""
        try {
            send(Frame.Text(message))
        } catch (e: Exception) {
            print("Error occurred while sending ${e.localizedMessage}")
            return
        }
    }
}

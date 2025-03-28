package com.example.messageTypes

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val username: String,
    val websocket: WebSocketServerSession? = null
)
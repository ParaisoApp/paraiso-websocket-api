package com.example.messageTypes

import io.ktor.server.websocket.WebSocketServerSession

data class User(
    val userId: String,
    val username: String,
    val websocket: WebSocketServerSession,
    var isReadyToChat: Boolean = false
)

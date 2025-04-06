package com.example.messageTypes

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val username: String,
    val role: UserRole,
    val banned: Boolean,
    val websocket: WebSocketServerSession? = null
) { companion object }
@Serializable
enum class UserRole {
    @SerialName("ADMIN")
    ADMIN,

    @SerialName("SYSTEM")
    SYSTEM,

    @SerialName("MOD")
    MOD,

    @SerialName("USER")
    USER,

    @SerialName("GUEST")
    GUEST,
}

fun User.Companion.createUnknown() = User(
    userId = "UNKNOWN",
    username = "UNKNOWN",
    banned = false,
    role = UserRole.GUEST,
    websocket = null
)
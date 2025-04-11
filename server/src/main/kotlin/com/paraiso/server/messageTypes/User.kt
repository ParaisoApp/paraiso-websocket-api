package com.paraiso.server.messageTypes

import com.paraiso.domain.util.Constants.UNKNOWN
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
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
    GUEST
}

fun User.Companion.createUnknown() =
    User(
        id = UNKNOWN,
        name = UNKNOWN,
        banned = false,
        roles = UserRole.GUEST,
        websocket = null,
        status = UserStatus.CONNECTED
    )

enum class UserStatus {
    CONNECTED,
    DISCONNECTED
}

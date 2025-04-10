package com.paraiso.server.messageTypes

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val roles: com.paraiso.server.messageTypes.UserRole,
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
    GUEST
}

fun com.paraiso.server.messageTypes.User.Companion.createUnknown() =
    _root_ide_package_.com.paraiso.server.messageTypes.User(
        id = "UNKNOWN",
        name = "UNKNOWN",
        banned = false,
        roles = _root_ide_package_.com.paraiso.server.messageTypes.UserRole.GUEST,
        websocket = null
    )

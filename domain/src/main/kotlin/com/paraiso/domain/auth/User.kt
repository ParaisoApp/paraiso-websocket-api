package com.paraiso.domain.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
    val lastSeen: Long
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

enum class UserStatus {
    CONNECTED,
    DISCONNECTED
}

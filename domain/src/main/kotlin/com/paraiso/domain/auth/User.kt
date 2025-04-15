package com.paraiso.domain.auth

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val posts: Map<String, Boolean>,
    val replies: Map<String, Boolean>,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
    val blockList: Set<String>,
    val image: String,
    val lastSeen: Long,
    val settings: UserSettings,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class UserSettings(
    val theme: Int,
    val accent: Int,
    val toolTips: Boolean,
    val postSubmit: Boolean
) { companion object }

fun UserSettings.Companion.initSettings() =
    UserSettings(
        theme = 0,
        accent = 0,
        toolTips = true,
        postSubmit = true
    )

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

@Serializable
enum class UserStatus {
    @SerialName("CONNECTED")
    CONNECTED,
    @SerialName("DISCONNECTED")
    DISCONNECTED
}

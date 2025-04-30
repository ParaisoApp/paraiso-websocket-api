package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val posts: Set<String>,
    val replies: Set<String>,
    val chats: Map<String, List<DirectMessage>>,
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

@Serializable
data class UserReturn(
    val id: String,
    val name: String,
    val posts: Map<String, Map<String, Boolean>>,
    val comments: Map<String, Map<String, Boolean>>,
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

fun User.toUserReturn(
    posts: Map<String, Map<String, Boolean>>,
    comments: Map<String, Map<String, Boolean>>
) =
    UserReturn(
        id = id,
        name = name,
        posts = posts,
        comments = comments,
        replies = replies.associateWith { true },
        roles = roles,
        banned = banned,
        status = status,
        blockList = blockList,
        image = image,
        lastSeen = lastSeen,
        settings = settings,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

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

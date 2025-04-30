package com.paraiso.com.paraiso.api.auth

import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UserStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import com.paraiso.domain.users.User as UserDomain

@Serializable
data class User(
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

fun UserDomain.toResponse() = User(
    id = id,
    name = name,
    posts = emptyMap(),
    comments = emptyMap(),
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
fun User.toDomain() = UserDomain(
    id = id,
    name = name,
    posts = emptySet(),
    replies = replies.keys,
    chats = emptyMap(),
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

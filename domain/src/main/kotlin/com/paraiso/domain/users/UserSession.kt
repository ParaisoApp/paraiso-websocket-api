package com.paraiso.domain.users

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserSession (
    val id: String,
    val userId: String,
    val serverId: String,
    val sessionIds: Set<String>
)

@Serializable
data class UserSessionResponse (
    val id: String,
    val userId: String,
    val serverId: String,
    val status: UserStatus,
    val sessionIds: Set<String>
)

fun UserSessionResponse.toDomain() =
    UserSession(
        id = id,
        userId = userId,
        serverId = serverId,
        sessionIds = sessionIds
    )

fun UserSession.toResponse(status: UserStatus) =
    UserSessionResponse(
        id = id,
        userId = userId,
        serverId = serverId,
        status = status,
        sessionIds = sessionIds
    )
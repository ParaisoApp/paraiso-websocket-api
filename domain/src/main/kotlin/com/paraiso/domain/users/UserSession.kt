package com.paraiso.domain.users

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserSession (
    val id: String,
    val userId: String,
    val serverId: String
)

@Serializable
data class UserSessionResponse (
    val id: String,
    val userId: String,
    val serverId: String,
    val status: UserStatus
)

fun UserSessionResponse.toDomain() =
    UserSession(
        id = id,
        userId = userId,
        serverId = serverId
    )

fun UserSession.toResponse(status: UserStatus) =
    UserSessionResponse(
        id = id,
        userId = userId,
        serverId = serverId,
        status = status
    )
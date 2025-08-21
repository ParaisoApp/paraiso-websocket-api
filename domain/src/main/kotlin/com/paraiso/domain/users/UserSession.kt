package com.paraiso.domain.users

import com.paraiso.domain.util.Constants.ID
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSession (
    @SerialName(ID) val id: String,
    val userId: String,
    val serverId: String,
    val status: UserStatus,
    val lastSeen: Instant
)

@Serializable
data class UserSessionResponse (
    val id: String,
    val userId: String,
    val serverId: String,
    val status: UserStatus,
    val lastSeen: Instant
)

fun UserSessionResponse.toDomain() =
    UserSession(
        id = id,
        userId = userId,
        serverId = serverId,
        status = status,
        lastSeen = lastSeen
    )

fun UserSession.toResponse() =
    UserSessionResponse(
        id = id,
        userId = userId,
        serverId = serverId,
        status = status,
        lastSeen = lastSeen
    )
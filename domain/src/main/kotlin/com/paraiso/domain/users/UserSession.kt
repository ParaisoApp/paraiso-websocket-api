package com.paraiso.domain.users

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val userId: String,
    val serverSessions: Map<String, Set<String>>,
)

@Serializable
data class UserSessionResponse(
    val id: String,
    val userId: String,
    val serverSessions: Map<String, Set<String>>,
    val status: UserStatus,
)

fun UserSessionResponse.toDomain() =
    UserSession(
        id = id,
        userId = userId,
        serverSessions = serverSessions
    )

fun UserSession.toResponse(status: UserStatus) =
    UserSessionResponse(
        id = id,
        userId = userId,
        serverSessions = serverSessions,
        status = status
    )

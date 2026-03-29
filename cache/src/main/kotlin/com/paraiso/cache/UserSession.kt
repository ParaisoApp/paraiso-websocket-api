package com.paraiso.cache

import com.paraiso.domain.users.UserSession as UserSessionDomain
import com.paraiso.domain.users.UserStatus
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val userId: String,
    val serverSessions: Map<String, Set<String>>
)

fun UserSessionDomain.toEntity() =
    UserSession(
        id = id,
        userId = userId,
        serverSessions = serverSessions
    )

fun UserSession.toDomain() =
    UserSessionDomain(
        id = id,
        userId = userId,
        serverSessions = serverSessions,
        status = UserStatus.CONNECTED
    )

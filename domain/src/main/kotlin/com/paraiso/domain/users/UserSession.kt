package com.paraiso.domain.users

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val userId: String,
    val serverSessions: Map<String, Set<String>>,
    val status: UserStatus
)

package com.paraiso.domain.util.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Login(
    val userId: String,
    val email: String,
    val password: String
)

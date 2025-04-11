package com.paraiso.domain.util.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Login(
    val email: String,
    val password: String
)

package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Login (
    val email: String,
    val password: String
)
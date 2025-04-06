package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Ban(
    val userId: String
)

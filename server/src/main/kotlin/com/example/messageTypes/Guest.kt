package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String,
    val name: String?
)

package com.paraiso.server.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String,
    val name: String?
)

fun randomGuestName() = "Guest ${(Math.random() * 10000).toInt()}"

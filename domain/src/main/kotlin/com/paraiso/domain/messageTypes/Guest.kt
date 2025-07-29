package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String,
    val name: String?
)


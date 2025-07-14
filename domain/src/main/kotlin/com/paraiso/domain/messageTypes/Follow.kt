package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    val sessionUserId: String,
    val userId: String
)

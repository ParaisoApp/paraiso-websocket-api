package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    val followerId: String,
    val followeeId: String
)

package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val userId: String,
    val tag: String
)

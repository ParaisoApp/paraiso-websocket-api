package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Ban(
    val userId: String
)

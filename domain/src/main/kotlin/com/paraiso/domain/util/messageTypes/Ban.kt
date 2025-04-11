package com.paraiso.domain.util.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Ban(
    val userId: String
)

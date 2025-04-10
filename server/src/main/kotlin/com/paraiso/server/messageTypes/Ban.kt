package com.paraiso.server.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Ban(
    val userId: String
)

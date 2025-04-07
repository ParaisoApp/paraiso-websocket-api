package com.paraiso.websocket.api.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Ban(
    val userId: String
)

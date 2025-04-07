package com.paraiso.websocket.api.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    val userId: String?,
    val userReceiveId: String?,
    val title: String?,
    val content: String?,
    val media: String?,
    val replyId: String?,
    val editId: String?
)

@Serializable
data class DirectMessage(
    val id: String? = null,
    val userId: String?,
    val userReceiveId: String?,
    val content: String?,
    val media: String?
)

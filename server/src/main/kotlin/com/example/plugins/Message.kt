package com.example.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping(
    val typeMapping: Map<MessageType, String>
)

@Serializable
data class Message(
    val userId: String?,
    val title: String?,
    val content: String?,
    val media: String?,
    val type: String? = null
)

fun Message.Companion.create(content: String, userId: String) = Message(
    title = "",
    content = content,
    userId = userId,
    media = ""
)

@Serializable
enum class MessageType() {
    @SerialName("msg")
    MSG,

    @SerialName("ping")
    PING,

    @SerialName("pong")
    PONG
}

package com.example.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val userId: String?,
    val title: String?,
    val content: String?,
    val media: String?,
    val type: MessageType?
)

fun Message.Companion.create(content: String, userId: String) = Message(
    title = "",
    content = content,
    userId = userId,
    media = "",
    type = MessageType.MSG
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

@Serializable
data class FooBar(
    val foo: String?,
    val bar: String?
)

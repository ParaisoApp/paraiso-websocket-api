package com.example.plugins

 import kotlinx.serialization.SerialName
 import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val userId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val media: String? = null,
    val type: MessageType? = null
)

fun Message.Companion.create(content: String, userId: String) = Message(
    title = "",
    content = content,
    userId = userId,
    media = "",
    type =  MessageType.MSG
)

@Serializable
enum class MessageType(){
    @SerialName("msg") MSG,
    @SerialName("ping") PING,
    @SerialName("pong") PONG
}

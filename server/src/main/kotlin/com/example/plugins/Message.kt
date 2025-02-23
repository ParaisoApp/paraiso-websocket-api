package com.example.plugins

 import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val title: String? = "",
    val content: String? = "",
    val userId: String? = "",
    val mediaUrl: String? = "",
    val type: String? = ""
)

fun Message.Companion.create(content: String, userId: String) = Message(
    title = "",
    content = content,
    userId = userId,
    mediaUrl = "",
    type = "msg"
)

package com.example.plugins

import kotlinx.serialization.Serializable


@Serializable
data class Message(
    val userId: String?,
    val title: String?,
    val content: String?,
    val media: String?
)

fun Message.Companion.create(content: String, userId: String) = Message(
    title = "",
    content = content,
    userId = userId,
    media = ""
)

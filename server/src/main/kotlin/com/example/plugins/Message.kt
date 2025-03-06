package com.example.plugins

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    val userId: String?,
    val title: String?,
    val content: String?,
    val media: String?,
    val replyId: String?,
    val editId: String?
)

package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Delete(
    val userId: String,
    val postId: String,
    val parentId: String
)

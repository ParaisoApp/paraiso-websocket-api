package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val userId: String,
    val postId: String,
    val upvote: Boolean
)

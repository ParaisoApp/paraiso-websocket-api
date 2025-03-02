package com.example.plugins

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val postId: String,
    val upvote: Boolean
)

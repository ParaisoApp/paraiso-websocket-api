package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val userId: String,
    val receiveUserId: String,
    val type: PostType,
    val postId: String,
    val upvote: Boolean
)

@Serializable
enum class PostType {
    SUPER,
    SUB,
    PROFILE,
    COMMENT,
    GAME
}

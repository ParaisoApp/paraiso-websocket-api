package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.PostType
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val voterId: String,
    val voteeId: String,
    val type: PostType,
    val postId: String,
    val upvote: Boolean
)

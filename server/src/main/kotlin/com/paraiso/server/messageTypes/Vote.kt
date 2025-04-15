package com.paraiso.server.messageTypes

import com.paraiso.domain.messageTypes.PostType
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val userId: String,
    val receiveUserId: String,
    val type: PostType,
    val postId: String,
    val upvote: Boolean
)

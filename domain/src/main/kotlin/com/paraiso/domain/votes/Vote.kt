package com.paraiso.domain.votes

import com.paraiso.domain.posts.PostType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val id: String? = null,
    val voterId: String,
    val voteeId: String? = null,
    val type: PostType,
    val postId: String,
    val upvote: Boolean,
    val score: Int? = 0,
    val createdOn: Instant? = Clock.System.now(),
    val updatedOn: Instant? = Clock.System.now()
)

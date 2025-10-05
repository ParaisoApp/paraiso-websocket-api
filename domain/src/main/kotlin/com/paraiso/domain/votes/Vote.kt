package com.paraiso.domain.votes

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    @SerialName(Constants.ID) val id: String? = null,
    val voterId: String,
    val voteeId: String? = null,
    val type: PostType,
    val postId: String,
    val upvote: Boolean,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

@Serializable
data class VoteResponse(
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

fun VoteResponse.toDomain() = Vote(
    id = id ?: "$voterId-$postId",
    voterId = voterId,
    voteeId = voteeId,
    type = type,
    postId = postId,
    upvote = upvote,
    createdOn = createdOn ?: Clock.System.now(),
    updatedOn = updatedOn ?: Clock.System.now()
)

fun Vote.toResponse() = VoteResponse(
    id = id,
    voterId = voterId,
    voteeId = voteeId,
    type = type,
    postId = postId,
    upvote = upvote,
    createdOn = createdOn,
    updatedOn = updatedOn
)

package com.paraiso.domain.posts

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostPin(
    @SerialName(Constants.ID) val id: String,
    val routeId: String,
    val postId: String,
    val order: Int,
    val userId: String,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant? = Clock.System.now()
)

@Serializable
data class PostPinResponse(
    val id: String,
    val routeId: String,
    val postId: String,
    val order: Int,
    val userId: String,
    val createdOn: Instant?
)

fun PostPinResponse.toResponse() = PostPin(
    id = id,
    routeId = routeId,
    postId = postId,
    order = order,
    userId = userId,
    createdOn = createdOn
)

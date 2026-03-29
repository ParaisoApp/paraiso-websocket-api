package com.paraiso.database.posts

import com.paraiso.domain.posts.PostPin as PostPinDomain
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostPin(
    @SerialName(ID) val id: String?,
    val routeId: String,
    val postId: String?,
    val order: Int,
    val userId: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant? = Clock.System.now()
)
fun PostPin.toDomain() = PostPinDomain(
    id = routeId,
    routeId = routeId,
    post = null,
    order = order,
    userId = userId,
    createdOn = createdOn
)
// id as route ID for now (with only one allowed pinned post)
fun PostPinDomain.toEntity() = PostPin(
    id = routeId,
    routeId = routeId,
    postId = post?.id,
    order = order,
    userId = userId,
    createdOn = createdOn
)
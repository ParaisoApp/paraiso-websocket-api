package com.paraiso.domain.follows

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    @SerialName(Constants.ID) val id: String,
    val followerId: String,
    val followeeId: String,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

@Serializable
data class FollowResponse(
    val id: String? = null,
    val followerId: String,
    val followeeId: String,
    val following: Boolean,
    val createdOn: Instant? = Clock.System.now(),
    val updatedOn: Instant? = Clock.System.now()
)
fun FollowResponse.toDomain() = Follow(
    id = id ?: "$followerId-$followeeId",
    followerId = followerId,
    followeeId = followeeId,
    createdOn = createdOn ?: Clock.System.now(),
    updatedOn = updatedOn ?: Clock.System.now()
)

fun Follow.toResponse() = FollowResponse(
    id = id,
    followerId = followerId,
    followeeId = followeeId,
    following = true,
    createdOn = createdOn ?: Clock.System.now(),
    updatedOn = updatedOn ?: Clock.System.now()
)

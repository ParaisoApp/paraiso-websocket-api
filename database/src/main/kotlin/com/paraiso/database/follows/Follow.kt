package com.paraiso.database.follows

import com.paraiso.domain.follows.Follow as FollowDomain
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
    val createdOn: Instant
)
fun FollowDomain.toEntity() = Follow(
    id = id ?: "$followerId-$followeeId",
    followerId = followerId,
    followeeId = followeeId,
    createdOn = createdOn ?: Clock.System.now()
)

fun Follow.toDomain() = FollowDomain(
    id = id,
    followerId = followerId,
    followeeId = followeeId,
    following = true,
    createdOn = createdOn
)

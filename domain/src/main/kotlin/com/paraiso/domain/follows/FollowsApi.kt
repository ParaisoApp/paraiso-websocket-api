package com.paraiso.domain.follows

import com.paraiso.domain.messageTypes.FollowResponse
import com.paraiso.domain.messageTypes.toDomain
import com.paraiso.domain.messageTypes.toResponse

class FollowsApi(
    private val followsDB: FollowsDB
) {

    suspend fun follow(follow: FollowResponse) =
        followsDB.save(listOf(follow.toDomain()))
    suspend fun unfollow(follow: FollowResponse) =
        followsDB.delete(follow.followerId, follow.followeeId)
    suspend fun get(followerId: String, followeeId: String) =
        followsDB.find(followerId, followeeId)
    suspend fun getByFollowerId(followerId: String) =
        followsDB.findByFollowerId(followerId).map { it.toResponse() }
    suspend fun getByFolloweeId(followeeId: String) =
        followsDB.findByFolloweeId(followeeId).map { it.toResponse() }
}

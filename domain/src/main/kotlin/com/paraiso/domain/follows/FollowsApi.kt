package com.paraiso.domain.follows

import com.paraiso.domain.messageTypes.FollowResponse
import com.paraiso.domain.messageTypes.toDomain
import com.paraiso.domain.messageTypes.toResponse

class FollowsApi(
    private val followsDB: FollowsDB
) {

    suspend fun follow(follow: FollowResponse) =
        if(follow.following){
            followsDB.delete(follow.followerId, follow.followeeId)
        }else{
            followsDB.save(listOf(follow.toDomain()))
        }
    suspend fun get(followerId: String, followeeId: String) =
        followsDB.find(followerId, followeeId)?.toResponse()
    suspend fun getByFollowerId(followerId: String) =
        followsDB.findByFollowerId(followerId).map { it.toResponse() }
    suspend fun getByFolloweeId(followeeId: String) =
        followsDB.findByFolloweeId(followeeId).map { it.toResponse() }
}

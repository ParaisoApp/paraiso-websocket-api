package com.paraiso.domain.follows

class FollowsApi(
    private val followsDB: FollowsDB
) {

    suspend fun follow(follow: FollowResponse) =
        if (follow.following) {
            followsDB.delete(follow.followerId, follow.followeeId)
        } else {
            followsDB.save(listOf(follow.toDomain()))
        }
    suspend fun findIn(followerId: String, followeeIds: List<String>) =
        followsDB.findIn(followerId, followeeIds).map { it.toResponse() }
    suspend fun getByFollowerId(followerId: String) =
        followsDB.findByFollowerId(followerId).map { it.toResponse() }
    suspend fun getByFolloweeId(followeeId: String) =
        followsDB.findByFolloweeId(followeeId).map { it.toResponse() }
}

package com.paraiso.domain.follows

class FollowsApi(
    private val followsDB: FollowsDB
) {

    suspend fun follow(follow: Follow) =
        if (follow.following) {
            followsDB.delete(follow.followerId, follow.followeeId)
        } else {
            followsDB.save(listOf(follow))
        }
    suspend fun findIn(followerId: String, followeeIds: List<String>) =
        followsDB.findIn(followerId, followeeIds)
    suspend fun getByFollowerId(followerId: String) =
        followsDB.findByFollowerId(followerId)
    suspend fun getByFolloweeId(followeeId: String) =
        followsDB.findByFolloweeId(followeeId)
}

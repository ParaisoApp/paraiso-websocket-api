package com.paraiso.domain.follows

import com.paraiso.domain.messageTypes.Follow

interface FollowsDB{
    suspend fun find(followerId: String, followeeId: String): Follow?
    suspend fun findIn(followerId: String, followeeIds: List<String>): List<Follow>
    suspend fun findByFollowerId(followerId: String): List<Follow>
    suspend fun findByFolloweeId(followeeId: String): List<Follow>
    suspend fun save(follows: List<Follow>): Int
    suspend fun delete(followerId: String, followeeId: String): Long
}

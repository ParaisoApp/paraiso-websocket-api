package com.paraiso.database.follows

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.follows.Follow
import com.paraiso.domain.follows.FollowsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class FollowsDBImpl(database: MongoDatabase) : FollowsDB {

    private val collection = database.getCollection("follows", Follow::class.java)

    override suspend fun find(followerId: String, followeeId: String) =
        collection.find(
            and(
                eq(Follow::followerId.name, followerId),
                eq(Follow::followeeId.name, followeeId)
            )
        ).firstOrNull()
    override suspend fun findIn(followerId: String, followeeIds: List<String>) =
        collection.find(
            and(
                eq(Follow::followerId.name, followerId),
                `in`(Follow::followerId.name, followeeIds)
            )
        ).toList()
    override suspend fun findByFollowerId(followerId: String) =
        collection.find(
            eq(Follow::followerId.name, followerId)
        ).toList()

    override suspend fun findByFolloweeId(followeeId: String) =
        collection.find(
            eq(Follow::followeeId.name, followeeId)
        ).toList()

    override suspend fun save(follows: List<Follow>): Int {
        val bulkOps = follows.map { follow ->
            ReplaceOneModel(
                eq(ID, follow.id),
                follow,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun delete(followerId: String, followeeId: String) =
        collection.deleteOne(
            and(
                eq(Follow::followerId.name, followerId),
                eq(Follow::followeeId.name, followerId)
            )
        ).deletedCount
}

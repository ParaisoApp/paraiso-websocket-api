package com.paraiso.database.follows

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.follows.Follow as FollowDomain
import com.paraiso.domain.follows.FollowsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class FollowsDBImpl(database: MongoDatabase) : FollowsDB {

    private val collection = database.getCollection("follows", Follow::class.java)

    override suspend fun findIn(followerId: String, followeeIds: List<String>) =
        withContext(Dispatchers.IO) {
            if (followeeIds.size == 1) {
                collection.find(
                    and(
                        eq(Follow::followerId.name, followerId),
                        eq(Follow::followeeId.name, followeeIds.firstOrNull())
                    )
                ).map { it.toDomain() }.toList()
            } else {
                collection.find(
                    and(
                        eq(Follow::followerId.name, followerId),
                        `in`(Follow::followerId.name, followeeIds)
                    )
                ).map { it.toDomain() }.toList()
            }
        }
    override suspend fun findByFollowerId(followerId: String) =
        withContext(Dispatchers.IO) {
            collection.find(
                eq(Follow::followerId.name, followerId)
            ).map { it.toDomain() }.toList()
        }

    override suspend fun findByFolloweeId(followeeId: String) =
        withContext(Dispatchers.IO) {
            collection.find(
                eq(Follow::followeeId.name, followeeId)
            ).map { it.toDomain() }.toList()
        }

    override suspend fun save(follows: List<FollowDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = follows.map { follow ->
                val entity = follow.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun delete(followerId: String, followeeId: String) =
        withContext(Dispatchers.IO) {
            collection.deleteOne(
                and(
                    eq(Follow::followerId.name, followerId),
                    eq(Follow::followeeId.name, followeeId)
                )
            ).deletedCount
        }
}

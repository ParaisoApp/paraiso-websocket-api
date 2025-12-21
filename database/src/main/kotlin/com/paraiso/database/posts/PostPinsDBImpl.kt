package com.paraiso.database.posts

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.posts.PostPinsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class PostPinsDBImpl(database: MongoDatabase) : PostPinsDB {

    private val collection = database.getCollection("postPins", PostPin::class.java)
    override suspend fun findByRouteId(routeId: String): List<PostPin> =
        withContext(Dispatchers.IO) {
            collection.find(`in`(PostPin::routeId.name, routeId)).toList()
        }

    override suspend fun save(postPins: List<PostPin>) =
        withContext(Dispatchers.IO) {
            val bulkOps = postPins.map { postPin ->
                ReplaceOneModel(
                    eq(ID, postPin.id),
                    postPin,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun delete(id: String) =
        withContext(Dispatchers.IO) {
            collection.deleteOne(eq(ID, id)).deletedCount
        }
}

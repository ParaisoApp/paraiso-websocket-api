package com.paraiso.database.posts

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.PostPin as PostPinDomain
import com.paraiso.domain.posts.PostPinsDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class PostPinsDBImpl(database: MongoDatabase) : PostPinsDB, Klogging {

    private val collection = database.getCollection("postPins", PostPin::class.java)
    override suspend fun findByRouteId(routeId: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(`in`(PostPin::routeId.name, routeId)).map { Pair(it.toDomain(), it.postId) }.toList()
            } catch (ex: Exception){
                logger.error { "Error finding post pins by route id: $ex" }
                emptyList()
            }
        }

    override suspend fun save(postPins: List<PostPinDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = postPins.map { postPin ->
                val entity = postPin.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
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

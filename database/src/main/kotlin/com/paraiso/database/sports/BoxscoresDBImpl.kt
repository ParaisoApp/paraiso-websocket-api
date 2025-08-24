package com.paraiso.database.sports

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.interfaces.BoxscoresDB
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class BoxscoresDBImpl(database: MongoDatabase) : BoxscoresDB {
    private val collection = database.getCollection("boxscores", BoxScore::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()

    override suspend fun findByIdsIn(ids: List<String>) =
        collection.find(`in`(ID, ids)).toList()

    override suspend fun save(boxscores: List<BoxScore>): Int {
        val bulkOps = boxscores.map { boxscore ->
            ReplaceOneModel(
                eq(ID, boxscore.id),
                boxscore,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

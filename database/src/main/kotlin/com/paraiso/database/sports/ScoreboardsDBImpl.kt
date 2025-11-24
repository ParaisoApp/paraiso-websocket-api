package com.paraiso.database.sports

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.ScoreboardEntity
import com.paraiso.domain.sport.interfaces.ScoreboardsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ScoreboardsDBImpl(database: MongoDatabase) : ScoreboardsDB {
    private val collection = database.getCollection("scoreboards", ScoreboardEntity::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun save(scoreboards: List<ScoreboardEntity>) =
        withContext(Dispatchers.IO) {
            val bulkOps = scoreboards.map { scoreboard ->
                ReplaceOneModel(
                    eq(ID, scoreboard.id),
                    scoreboard,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

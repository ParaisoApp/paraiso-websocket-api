package com.paraiso.database.sports

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.ScoreboardsDBAdapter
import com.paraiso.domain.sport.data.ScoreboardEntity
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class ScoreboardsDBAdapterImpl(database: MongoDatabase) : ScoreboardsDBAdapter {
    private val collection = database.getCollection("scoreboards", ScoreboardEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()

    override suspend fun save(scoreboards: List<ScoreboardEntity>): Int {
        val bulkOps = scoreboards.map { scoreboard ->
            ReplaceOneModel(
                eq(ID, scoreboard.id),
                scoreboard,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

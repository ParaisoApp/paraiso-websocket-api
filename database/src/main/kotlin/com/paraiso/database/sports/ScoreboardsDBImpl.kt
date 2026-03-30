package com.paraiso.database.sports

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Scoreboard
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.interfaces.ScoreboardsDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ScoreboardsDBImpl(database: MongoDatabase) : ScoreboardsDB, Klogging {
    private val collection = database.getCollection("scoreboards", Scoreboard::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(ID, id)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding scoreboard by id: $ex" }
                null
            }
        }

    override suspend fun save(scoreboards: List<ScoreboardDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = scoreboards.map { scoreboard ->
                val entity = scoreboard.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

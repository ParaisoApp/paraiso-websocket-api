package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.BoxScore
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.interfaces.BoxscoresDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class BoxscoresDBImpl(database: MongoDatabase) : BoxscoresDB, Klogging {
    private val collection = database.getCollection("boxscores", BoxScore::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try{
                if (ids.size == 1) {
                    collection.find(
                        Filters.and(
                            Filters.eq(ID, ids.firstOrNull())
                        )
                    ).map { it.toDomain() }.toList()
                } else {
                    collection.find(
                        Filters.and(
                            `in`(ID, ids)
                        )
                    ).map { it.toDomain() }.toList()
                }
            } catch (ex: Exception){
                logger.error { "Error finding boxscores by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun save(boxscores: List<BoxScoreDomain>) =
        withContext(Dispatchers.IO) {
            val allExisting = findByIdIn(boxscores.map { it.id }).associateBy { it.id }
            val now = Clock.System.now()
            val bulkOps = boxscores.map { boxScore ->
                val existing = allExisting[boxScore.id]
                val entity = boxScore.copy(
                    createdOn = existing?.createdOn ?: now,
                    updatedOn = now
                ).toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

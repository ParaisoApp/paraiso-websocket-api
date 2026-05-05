package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Playoff
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Playoff as PlayoffDomain
import com.paraiso.domain.sport.interfaces.PlayoffsDB
import com.paraiso.domain.util.Constants
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class PlayoffsDBImpl(database: MongoDatabase) : PlayoffsDB, Klogging {
    private val collection = database.getCollection("playoffs", Playoff::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try{
                if (ids.size == 1) {
                    collection.find(
                        Filters.and(
                            Filters.eq(Constants.ID, ids.firstOrNull())
                        )
                    ).map { it.toDomain() }.toList()
                } else {
                    collection.find(
                        Filters.and(
                            Filters.`in`(Constants.ID, ids)
                        )
                    ).map { it.toDomain() }.toList()
                }
            } catch (ex: Exception){
                logger.error { "Error finding playoffs by id: $ex" }
                emptyList()
            }
        }

    override suspend fun findBySportAndYear(
        sport: String,
        year: Int
    ) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    Filters.and(
                        Filters.eq(Playoff::sport.name, sport),
                        Filters.eq(Playoff::year.name, year)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding playoffs by sport and year: $ex" }
                null
            }
        }

    override suspend fun save(playoffs: List<PlayoffDomain>) =
        withContext(Dispatchers.IO) {
            val allExisting = findByIdIn(playoffs.map { it.id }).associateBy { it.id }
            val now = Clock.System.now()
            val bulkOps = playoffs.map { playoff ->
                val existing = allExisting[playoff.id]
                val entity = playoff.copy(
                    createdOn = existing?.createdOn ?: now,
                    updatedOn = now
                ).toEntity()
                ReplaceOneModel(
                    Filters.eq(Constants.ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

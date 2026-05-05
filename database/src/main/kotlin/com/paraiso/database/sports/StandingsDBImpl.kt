package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.AllStandings
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.interfaces.StandingsDB
import com.paraiso.domain.util.Constants
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class StandingsDBImpl(database: MongoDatabase) : StandingsDB, Klogging {
    private val collection = database.getCollection("standings", AllStandings::class.java)

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
                logger.error { "Error finding standings by id: $ex" }
                emptyList()
            }
        }

    override suspend fun save(allStandings: List<AllStandingsDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = allStandings.map { allStanding ->
                val entity = allStanding.toEntity()
                ReplaceOneModel(
                    Filters.eq(Constants.ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

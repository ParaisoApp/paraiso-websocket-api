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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class StandingsDBImpl(database: MongoDatabase) : StandingsDB {
    private val collection = database.getCollection("standings", AllStandings::class.java)

    override suspend fun findById(sport: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(Constants.ID, sport)).limit(1).firstOrNull()?.toDomain()
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

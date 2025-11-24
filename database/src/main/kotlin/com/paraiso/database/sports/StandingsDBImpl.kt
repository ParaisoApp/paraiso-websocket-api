package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.sport.interfaces.StandingsDB
import com.paraiso.domain.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class StandingsDBImpl(database: MongoDatabase) : StandingsDB {
    private val collection = database.getCollection("standings", AllStandings::class.java)

    override suspend fun findById(sport: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(Constants.ID, sport)).limit(1).firstOrNull()
        }

    override suspend fun save(allStandings: List<AllStandings>) =
        withContext(Dispatchers.IO) {
            val bulkOps = allStandings.map { allStanding ->
                ReplaceOneModel(
                    Filters.eq(Constants.ID, allStanding.id),
                    allStanding,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

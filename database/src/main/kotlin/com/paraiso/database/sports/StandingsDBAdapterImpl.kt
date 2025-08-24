package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.util.Constants
import kotlinx.coroutines.flow.firstOrNull

class StandingsDBAdapterImpl(database: MongoDatabase) : StandingsDBAdapter {
    private val collection = database.getCollection("standings", AllStandings::class.java)

    override suspend fun findById(sport: String) =
        collection.find(Filters.eq(Constants.ID, sport)).firstOrNull()

    override suspend fun save(allStandings: List<AllStandings>): Int {
        val bulkOps = allStandings.map { allStanding ->
            ReplaceOneModel(
                Filters.eq(Constants.ID, allStanding.id),
                allStanding,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

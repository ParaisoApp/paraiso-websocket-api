package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.sport.data.Standings
import com.paraiso.domain.util.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class StandingsDBAdapterImpl(database: MongoDatabase) : StandingsDBAdapter {
    private val collection = database.getCollection("standings", AllStandings::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(Constants.ID, id)).firstOrNull()

    override suspend fun save(allStandings: List<AllStandings>) =
        collection.insertMany(allStandings).insertedIds.map { it.toString() }
}

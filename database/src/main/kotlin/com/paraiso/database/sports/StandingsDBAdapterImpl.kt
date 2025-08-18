package com.paraiso.database.sports

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.data.Standings

class StandingsDBAdapterImpl(database: MongoDatabase) : StandingsDBAdapter {
    private val collection = database.getCollection("standings", Standings::class.java)

    suspend fun getAll() =
        collection.find()

    suspend fun save(standings: List<Standings>) =
        collection.insertMany(standings)
}

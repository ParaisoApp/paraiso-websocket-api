package com.paraiso.database.sports.bball

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallStandingsDBAdapter
import com.paraiso.domain.sport.data.Standings

class BBallStandingsDBAdapterImpl(database: MongoDatabase) : BBallStandingsDBAdapter {
    private val collection = database.getCollection("bballStandings", Standings::class.java)

    suspend fun getAll() =
        collection.find()

    suspend fun save(standings: List<Standings>) =
        collection.insertMany(standings)
}

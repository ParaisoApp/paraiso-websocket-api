package com.paraiso.database.sports.fball

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallStandingsDBAdapter
import com.paraiso.domain.sport.adapters.fball.FBallStandingsDBAdapter
import com.paraiso.domain.sport.data.Standings

class FBallStandingsDBAdapterImpl(database: MongoDatabase) : FBallStandingsDBAdapter {
    private val collection = database.getCollection("fballStandings", Standings::class.java)

    suspend fun getAll() =
        collection.find()

    suspend fun save(standings: List<Standings>) =
        collection.insertMany(standings)
}

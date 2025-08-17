package com.paraiso.database.sports.fball

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallTeamsDBAdapter
import com.paraiso.domain.sport.adapters.fball.FBallTeamsDBAdapter
import com.paraiso.domain.sport.data.StatLeaders

class FBallLeadersDBAdapterImpl(database: MongoDatabase) : FBallTeamsDBAdapter {
    private val collection = database.getCollection("fballLeaders", StatLeaders::class.java)

    fun getAll() =
        collection.find()

    suspend fun save(statLeaders: List<StatLeaders>) =
        collection.insertMany(statLeaders)
}

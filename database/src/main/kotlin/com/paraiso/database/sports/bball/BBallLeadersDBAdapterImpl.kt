package com.paraiso.database.sports.bball

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallTeamsDBAdapter
import com.paraiso.domain.sport.data.StatLeaders

class BBallLeadersDBAdapterImpl(database: MongoDatabase) : BBallTeamsDBAdapter {
    private val collection = database.getCollection("bballLeaders", StatLeaders::class.java)

    fun getAll() =
        collection.find()

    suspend fun save(statLeaders: List<StatLeaders>) =
        collection.insertMany(statLeaders)
}

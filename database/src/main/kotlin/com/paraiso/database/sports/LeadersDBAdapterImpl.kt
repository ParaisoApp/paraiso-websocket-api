package com.paraiso.database.sports

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.StatLeaders

class LeadersDBAdapterImpl(database: MongoDatabase) : LeadersDBAdapter {
    private val collection = database.getCollection("leaders", StatLeaders::class.java)

    fun getAll() =
        collection.find()

    suspend fun save(statLeaders: List<StatLeaders>) =
        collection.insertMany(statLeaders)
}

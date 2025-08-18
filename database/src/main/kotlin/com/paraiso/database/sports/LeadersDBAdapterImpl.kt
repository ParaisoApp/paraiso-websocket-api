package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.StatLeaders
import com.paraiso.domain.util.Constants

class LeadersDBAdapterImpl(database: MongoDatabase) : LeadersDBAdapter {
    private val collection = database.getCollection("leaders", StatLeaders::class.java)

    fun getAll() =
        collection.find()

    suspend fun save(statLeaders: List<StatLeaders>): Int {
        val bulkOps = statLeaders.map { statLeader ->
            ReplaceOneModel(
                Filters.eq(Constants.ID, statLeader.id),
                statLeader,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

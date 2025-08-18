package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.RostersDBAdapter
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.RosterEntity
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class RostersDBAdapterImpl(database: MongoDatabase) : RostersDBAdapter {
    private val collection = database.getCollection("rosters", RosterEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun save(rosters: List<RosterEntity>): Int {
        val bulkOps = rosters.map { roster ->
            ReplaceOneModel(
                Filters.eq(ID, roster.id),
                roster,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

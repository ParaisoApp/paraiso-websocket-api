package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.CoachesDBAdapter
import com.paraiso.domain.sport.data.Coach
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class CoachesDBAdapterImpl(database: MongoDatabase) : CoachesDBAdapter {
    private val collection = database.getCollection("coaches", Coach::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun save(coaches: List<Coach>): Int {
        val bulkOps = coaches.map { coach ->
            ReplaceOneModel(
                Filters.eq(ID, coach.id),
                coach,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

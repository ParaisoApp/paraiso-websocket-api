package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.Coach
import com.paraiso.domain.sport.interfaces.CoachesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class CoachesDBImpl(database: MongoDatabase) : CoachesDB {
    private val collection = database.getCollection("coaches", Coach::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun save(coaches: List<Coach>) =
        withContext(Dispatchers.IO) {
            val bulkOps = coaches.map { coach ->
                ReplaceOneModel(
                    Filters.eq(ID, coach.id),
                    coach,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

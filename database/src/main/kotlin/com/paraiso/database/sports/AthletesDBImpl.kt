package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.Athlete
import com.paraiso.domain.sport.interfaces.AthletesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class AthletesDBImpl(database: MongoDatabase) : AthletesDB {
    private val collection = database.getCollection("athletes", Athlete::class.java)

    override suspend fun findByIdsIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            collection.find(`in`(ID, ids)).toList()
        }

    override suspend fun save(athletes: List<Athlete>) =
        withContext(Dispatchers.IO) {
            val bulkOps = athletes.map { athlete ->
                ReplaceOneModel(
                    Filters.eq(ID, athlete.id),
                    athlete,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

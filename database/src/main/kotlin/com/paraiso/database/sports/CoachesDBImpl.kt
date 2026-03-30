package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Coach
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Coach as CoachDomain
import com.paraiso.domain.sport.interfaces.CoachesDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class CoachesDBImpl(database: MongoDatabase) : CoachesDB, Klogging {
    private val collection = database.getCollection("coaches", Coach::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding coach by id: $ex" }
                null
            }
        }

    override suspend fun save(coaches: List<CoachDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = coaches.map { coach ->
                val entity = coach.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

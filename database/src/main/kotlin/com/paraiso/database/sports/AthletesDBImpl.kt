package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Athlete
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Athlete as AthleteDomain
import com.paraiso.domain.sport.interfaces.AthletesDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class AthletesDBImpl(database: MongoDatabase) : AthletesDB, Klogging {
    private val collection = database.getCollection("athletes", Athlete::class.java)

    override suspend fun findByIdsIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(`in`(ID, ids)).map { it.toDomain() }.toList()
            } catch (ex: Exception){
                logger.error { "Error finding athletes by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun save(athletes: List<AthleteDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = athletes.map { athlete ->
                val entity = athlete.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

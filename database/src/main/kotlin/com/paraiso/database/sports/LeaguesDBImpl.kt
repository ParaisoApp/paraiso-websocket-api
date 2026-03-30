package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.League
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.League as LeagueDomain
import com.paraiso.domain.sport.interfaces.LeaguesDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class LeaguesDBImpl(database: MongoDatabase) : LeaguesDB, Klogging {
    private val collection = database.getCollection("leagues", League::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding league by id: $ex" }
                null
            }
        }

    override suspend fun findBySport(sport: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(Filters.eq(League::sport.name, sport)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding league by sport: $ex" }
                null
            }
        }

    override suspend fun save(leagues: List<LeagueDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = leagues.map { league ->
                val entity = league.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

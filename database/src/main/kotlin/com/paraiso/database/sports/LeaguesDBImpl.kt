package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.League
import com.paraiso.domain.sport.interfaces.LeaguesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class LeaguesDBImpl(database: MongoDatabase) : LeaguesDB {
    private val collection = database.getCollection("leagues", League::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun findBySport(sport: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(League::sport.name, sport)).limit(1).firstOrNull()
        }

    override suspend fun save(leagues: List<League>) =
        withContext(Dispatchers.IO) {
            val bulkOps = leagues.map { league ->
                ReplaceOneModel(
                    Filters.eq(ID, league.id),
                    league,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

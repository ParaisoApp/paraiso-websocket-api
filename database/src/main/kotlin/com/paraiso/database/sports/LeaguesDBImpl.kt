package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.League
import com.paraiso.domain.sport.interfaces.LeaguesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class LeaguesDBImpl(database: MongoDatabase) : LeaguesDB {
    private val collection = database.getCollection("leagues", League::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun findBySport(sport: String) =
        collection.find(Filters.eq(League::sport.name, sport)).firstOrNull()

    override suspend fun save(leagues: List<League>): Int {
        val bulkOps = leagues.map { league ->
            ReplaceOneModel(
                Filters.eq(ID, league.id),
                league,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

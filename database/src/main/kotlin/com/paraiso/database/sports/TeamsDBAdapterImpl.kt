package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.Team
import kotlinx.coroutines.flow.firstOrNull

class TeamsDBAdapterImpl(database: MongoDatabase) : TeamsDBAdapter {
    private val collection = database.getCollection("teams", Team::class.java)


    suspend fun findByAbbr(abbr: String) =
        collection.find(Filters.eq(Team::abbreviation.name, abbr)).firstOrNull()
    fun getAll() =
        collection.find()

    suspend fun save(teams: List<Team>) =
        collection.insertMany(teams)
}

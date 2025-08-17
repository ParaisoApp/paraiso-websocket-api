package com.paraiso.database.sports.bball

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallTeamsDBAdapter
import com.paraiso.domain.sport.data.Team
import kotlinx.coroutines.flow.firstOrNull

class BBallTeamsDBAdapterImpl(database: MongoDatabase) : BBallTeamsDBAdapter {
    private val collection = database.getCollection("bballTeams", Team::class.java)


    suspend fun findByAbbr(abbr: String) =
        collection.find(Filters.eq(Team::abbreviation.name, abbr)).firstOrNull()
    fun getAll() =
        collection.find()

    suspend fun save(teams: List<Team>) =
        collection.insertMany(teams)
}

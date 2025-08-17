package com.paraiso.database.sports.bball

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallRostersDBAdapter
import com.paraiso.domain.sport.data.Roster
import kotlinx.coroutines.flow.firstOrNull

class BBallRostersDBAdapterImpl(database: MongoDatabase) : BBallRostersDBAdapter {
    private val collection = database.getCollection("bballRosters", Roster::class.java)

    suspend fun findByTeamId(id: String) =
        collection.find(Filters.eq("${Roster::team}.id", id)).firstOrNull()

    suspend fun save(rosters: List<Roster>) =
        collection.insertMany(rosters)
}

package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.RostersDBAdapter
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class RostersDBAdapterImpl(database: MongoDatabase) : RostersDBAdapter {
    private val collection = database.getCollection("rosters", Roster::class.java)

    suspend fun findByTeamId(id: String) =
        collection.find(Filters.eq("${Roster::team}.$ID", id)).firstOrNull()

    suspend fun save(rosters: List<Roster>) =
        collection.insertMany(rosters)
}

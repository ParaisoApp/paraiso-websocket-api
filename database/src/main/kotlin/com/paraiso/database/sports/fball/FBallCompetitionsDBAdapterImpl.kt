package com.paraiso.database.sports.fball

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallCompetitionsDBAdapter
import com.paraiso.domain.sport.adapters.bball.BBallSchedulesDBAdapter
import com.paraiso.domain.sport.adapters.fball.FBallCompetitionsDBAdapter
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Schedule
import kotlinx.coroutines.flow.firstOrNull

class FBallCompetitionsDBAdapterImpl(database: MongoDatabase) : FBallCompetitionsDBAdapter {
    private val collection = database.getCollection("fballCompetitions", Competition::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(Competition::id.name, id)).firstOrNull()

    suspend fun save(competitions: List<Competition>) =
        collection.insertMany(competitions)
}

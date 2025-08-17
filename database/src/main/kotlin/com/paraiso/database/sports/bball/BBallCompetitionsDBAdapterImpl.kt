package com.paraiso.database.sports.bball

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallCompetitionsDBAdapter
import com.paraiso.domain.sport.adapters.bball.BBallSchedulesDBAdapter
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class BBallCompetitionsDBAdapterImpl(database: MongoDatabase) : BBallCompetitionsDBAdapter {
    private val collection = database.getCollection("bballCompetitions", Competition::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    suspend fun save(competitions: List<Competition>) =
        collection.insertMany(competitions)
}

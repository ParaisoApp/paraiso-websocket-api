package com.paraiso.database.sports.fball

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.bball.BBallSchedulesDBAdapter
import com.paraiso.domain.sport.adapters.fball.FBallSchedulesDBAdapter
import com.paraiso.domain.sport.data.Schedule
import kotlinx.coroutines.flow.firstOrNull

class FBallSchedulesDBAdapterImpl(database: MongoDatabase) : FBallSchedulesDBAdapter {
    private val collection = database.getCollection("fballSchedules", Schedule::class.java)

    suspend fun findByTeamId(id: String) =
        collection.find(Filters.eq("${Schedule::team}.id", id)).firstOrNull()

    suspend fun save(schedules: List<Schedule>) =
        collection.insertMany(schedules)
}

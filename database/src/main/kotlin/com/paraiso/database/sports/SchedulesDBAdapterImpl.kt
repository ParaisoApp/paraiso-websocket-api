package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.SchedulesDBAdapter
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class SchedulesDBAdapterImpl(database: MongoDatabase) : SchedulesDBAdapter {
    private val collection = database.getCollection("schedules", Schedule::class.java)

    suspend fun findByTeamId(id: String) =
        collection.find(Filters.eq("${Schedule::team}.$ID", id)).firstOrNull()

    suspend fun save(schedules: List<Schedule>) =
        collection.insertMany(schedules)
}

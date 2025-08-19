package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.SchedulesDBAdapter
import com.paraiso.domain.sport.data.ScheduleEntity
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class SchedulesDBAdapterImpl(database: MongoDatabase) : SchedulesDBAdapter {
    private val collection = database.getCollection("schedules", ScheduleEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun save(schedules: List<ScheduleEntity>): Int {
        val bulkOps = schedules.map { schedule ->
            ReplaceOneModel(
                Filters.eq(ID, schedule.id),
                schedule,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}

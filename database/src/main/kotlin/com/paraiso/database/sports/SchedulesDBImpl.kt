package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.interfaces.SchedulesDB
import com.paraiso.domain.sport.data.ScheduleEntity
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class SchedulesDBImpl(database: MongoDatabase) : SchedulesDB {
    private val collection = database.getCollection("schedules", ScheduleEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int,
    ): ScheduleEntity? =
        collection.find(
            Filters.and(
                Filters.eq(ScheduleEntity::sport.name, sport),
                Filters.eq(ScheduleEntity::teamId.name, teamId),
                Filters.eq("${ScheduleEntity::season.name}.year", seasonYear),
                Filters.eq("${ScheduleEntity::season.name}.type", seasonType)
            )
        ).firstOrNull()

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

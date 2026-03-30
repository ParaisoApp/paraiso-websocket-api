package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Schedule
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.interfaces.SchedulesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SchedulesDBImpl(database: MongoDatabase) : SchedulesDB {
    private val collection = database.getCollection("schedules", Schedule::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()?.toDomain()
        }

    override suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.find(
                Filters.and(
                    Filters.eq(Schedule::sport.name, sport),
                    Filters.eq(Schedule::teamId.name, teamId),
                    Filters.eq("${Schedule::season.name}.year", seasonYear),
                    Filters.eq("${Schedule::season.name}.type", seasonType)
                )
            ).limit(1).firstOrNull()?.toDomain()
        }

    override suspend fun save(schedules: List<ScheduleDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = schedules.map { schedule ->
                val entity = schedule.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

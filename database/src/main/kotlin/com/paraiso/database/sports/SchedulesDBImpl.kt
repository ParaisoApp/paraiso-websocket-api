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
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SchedulesDBImpl(database: MongoDatabase) : SchedulesDB, Klogging {
    private val collection = database.getCollection("schedules", Schedule::class.java)

    override suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ): Pair<ScheduleDomain?, List<String>>? =
        withContext(Dispatchers.IO) {
            try{
                val schedule = collection.find(
                    Filters.and(
                        Filters.eq(Schedule::sport.name, sport),
                        Filters.eq(Schedule::teamId.name, teamId),
                        Filters.eq("${Schedule::season.name}.year", seasonYear),
                        Filters.eq("${Schedule::season.name}.type", seasonType)
                    )
                ).limit(1).firstOrNull()
                Pair(schedule?.toDomain(), schedule?.events ?: emptyList())
            } catch (ex: Exception){
                logger.error { "Error finding schedule by sport, teamId, season, and type: $ex" }
                null
            }
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

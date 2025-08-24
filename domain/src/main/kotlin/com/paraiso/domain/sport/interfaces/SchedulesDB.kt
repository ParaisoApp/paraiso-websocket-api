package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.ScheduleEntity

interface SchedulesDB {
    suspend fun findById(id: String): ScheduleEntity?
    suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ): ScheduleEntity?
    suspend fun save(schedules: List<ScheduleEntity>): Int
}

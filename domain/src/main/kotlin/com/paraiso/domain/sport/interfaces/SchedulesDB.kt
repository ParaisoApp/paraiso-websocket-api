package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Schedule


interface SchedulesDB {
    suspend fun findById(id: String): Schedule?
    suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ): Schedule?
    suspend fun save(schedules: List<Schedule>): Int
}

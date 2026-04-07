package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Schedule


interface SchedulesDB {
    suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ): Pair<Schedule?, List<String>>?
    suspend fun save(schedules: List<Schedule>): Int
}

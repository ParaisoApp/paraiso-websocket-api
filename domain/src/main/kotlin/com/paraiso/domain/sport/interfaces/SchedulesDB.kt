package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Athlete
import com.paraiso.domain.sport.data.Schedule


interface SchedulesDB {
    suspend fun findByIdIn(ids: List<String>): List<Pair<Schedule?, List<String>>>
    suspend fun findBySportAndTeamIdAndYearAndType(
        sport: String,
        teamId: String,
        seasonYear: Int,
        seasonType: Int
    ): Pair<Schedule?, List<String>>?
    suspend fun save(schedules: List<Schedule>): Int
}

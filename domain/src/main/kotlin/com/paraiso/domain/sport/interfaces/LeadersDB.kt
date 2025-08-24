package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.StatLeaders

interface LeadersDB {
    suspend fun findBySport(sport: String): StatLeaders?
    suspend fun findBySportAndSeasonAndType(
        sport: String,
        season: String,
        type: String
    ): StatLeaders?
    suspend fun save(statLeaders: List<StatLeaders>): Int
}

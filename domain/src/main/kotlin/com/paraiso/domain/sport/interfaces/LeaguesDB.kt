package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.League

interface LeaguesDB {
    suspend fun findByIdIn(ids: List<String>): List<League>
    suspend fun findBySport(sport: String): League?
    suspend fun save(leagues: List<League>): Int
}

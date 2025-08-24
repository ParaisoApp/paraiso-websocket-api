package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.League

interface LeaguesDB {
    suspend fun findById(id: String): League?
    suspend fun findBySport(sport: String): League?
    suspend fun save(leagues: List<League>): Int
}

package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.League

interface LeaguesDBAdapter {
    suspend fun findById(id: String): League?
    suspend fun findBySport(sport: String): League?
    suspend fun save(leagues: List<League>): Int
}

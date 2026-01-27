package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Playoff

interface PlayoffsDB {
    suspend fun findById(id: String): Playoff?
    suspend fun findBySportAndYear(sport: String, year: Int): Playoff?
    suspend fun save(playoffs: List<Playoff>): Int
}

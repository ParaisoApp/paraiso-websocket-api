package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Playoff

interface PlayoffsDB {
    suspend fun findByIdIn(ids: List<String>): List<Playoff>
    suspend fun findBySportAndYear(sport: String, year: Int): Playoff?
    suspend fun save(playoffs: List<Playoff>): Int
}

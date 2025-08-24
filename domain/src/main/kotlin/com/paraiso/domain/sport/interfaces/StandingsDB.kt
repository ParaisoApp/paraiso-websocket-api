package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.AllStandings

interface StandingsDB {
    suspend fun findById(sport: String): AllStandings?
    suspend fun save(allStandings: List<AllStandings>): Int
}

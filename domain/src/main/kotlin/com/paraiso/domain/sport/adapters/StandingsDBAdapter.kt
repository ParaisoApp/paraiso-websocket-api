package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.AllStandings

interface StandingsDBAdapter {
    suspend fun findById(sport: String): AllStandings?
    suspend fun save(allStandings: List<AllStandings>): Int
}

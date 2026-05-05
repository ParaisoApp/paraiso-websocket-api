package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.AllStandings

interface StandingsDB {
    suspend fun findByIdIn(ids: List<String>): List<AllStandings>
    suspend fun save(allStandings: List<AllStandings>): Int
}

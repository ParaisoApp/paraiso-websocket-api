package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Team

interface TeamsDB {
    suspend fun findById(id: String): Team?
    suspend fun findBySportAndAbbr(sport: String, abbr: String): Team?
    suspend fun findBySport(sport: String): List<Team>
    suspend fun save(teams: List<Team>): Int
}

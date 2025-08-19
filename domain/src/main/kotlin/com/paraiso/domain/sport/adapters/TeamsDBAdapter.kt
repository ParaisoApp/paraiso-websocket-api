package com.paraiso.domain.sport.adapters

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Team

interface TeamsDBAdapter {
    suspend fun findById(id: String): Team?
    suspend fun findBySport(sport: SiteRoute): List<Team>
    suspend fun save(teams: List<Team>): Int
}

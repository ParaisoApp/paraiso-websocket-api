package com.paraiso.domain.sport.adapters

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.RosterEntity

interface RostersDBAdapter {
    suspend fun findById(id: String): RosterEntity?
    suspend fun findBySportAndTeamId(sport: String, teamId: String): RosterEntity?
    suspend fun save(rosters: List<RosterEntity>): Int
}

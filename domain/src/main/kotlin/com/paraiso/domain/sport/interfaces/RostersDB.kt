package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.RosterEntity

interface RostersDB {
    suspend fun findById(id: String): RosterEntity?
    suspend fun findBySportAndTeamId(sport: String, teamId: String): RosterEntity?
    suspend fun save(rosters: List<RosterEntity>): Int
}

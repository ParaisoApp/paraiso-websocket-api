package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Roster

interface RostersDB {
    suspend fun findById(id: String): Roster?
    suspend fun findBySportAndTeamId(sport: String, teamId: String): Triple<Roster?, List<String>, String?>
    suspend fun save(rosters: List<Roster>): Int
}

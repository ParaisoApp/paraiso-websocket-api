package com.paraiso.domain.sport

import com.paraiso.domain.sport.sports.AllStandings
import com.paraiso.domain.sport.sports.BoxScore
import com.paraiso.domain.sport.sports.Roster
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.sport.sports.StatLeaders
import com.paraiso.domain.sport.sports.Team

interface SportOperation {

    suspend fun getScoreboard(): Scoreboard?
    suspend fun getGameStats(gameId: String): BoxScore?
    suspend fun getStandings(): AllStandings?
    suspend fun getTeams(): List<Team>
    suspend fun getRoster(teamId: String): Roster?
    suspend fun getLeaders(): StatLeaders?
    suspend fun getSchedule(teamId: String): StatLeaders?
}

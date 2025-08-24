package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.League
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.StatLeaders
import com.paraiso.domain.sport.data.Team

interface SportClient {
    suspend fun getLeague(sport: SiteRoute): League?
    suspend fun getScoreboard(sport: SiteRoute): Scoreboard?
    suspend fun getGameStats(sport: SiteRoute, competitionId: String): BoxScore?
    suspend fun getStandings(sport: SiteRoute): AllStandings?
    suspend fun getTeams(sport: SiteRoute): List<Team>
    suspend fun getRoster(sport: SiteRoute, teamId: String): Roster?
    suspend fun getLeaders(
        sport: SiteRoute,
        season: String,
        type: String
    ): StatLeaders?
    suspend fun getTeamLeaders(
        sport: SiteRoute,
        season: String,
        type: String,
        teamId: String
    ): StatLeaders?
    suspend fun getSchedule(sport: SiteRoute, teamId: String): Schedule?
}

package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.League
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.StatLeaders
import com.paraiso.domain.sport.data.Team

interface FBallOperation {
    suspend fun getLeague(): League?
    suspend fun getScoreboard(): Scoreboard?
    suspend fun getGameStats(competitionId: String): BoxScore?
    suspend fun getStandings(): AllStandings?
    suspend fun getTeams(): List<Team>
    suspend fun getRoster(teamId: String): Roster?
    suspend fun getLeaders(season: String, type: String): StatLeaders?
    suspend fun getSchedule(teamId: String): Schedule?
}

package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.toResponse


class FBallApi(
    private val teamsDBAdapter: TeamsDBAdapter,
    private val standingsDBAdapter: StandingsDBAdapter,
    private val leadersDBAdapter: LeadersDBAdapter
) {
    suspend fun getTeamByAbbr(teamAbbr: String) = teamsDBAdapter.findById("${SiteRoute.FOOTBALL}-$teamAbbr")?.toResponse()
    suspend fun getTeams() = teamsDBAdapter.findBySport(SiteRoute.FOOTBALL).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = standingsDBAdapter.findById(SiteRoute.FOOTBALL.toString())?.standingsGroups?.flatMap { confGroup ->
        confGroup.subGroups
    }?.associate { standingsSubGroup ->
        standingsSubGroup.divName to standingsSubGroup.standings.map { it.toResponse() }
    }
    suspend fun getLeaders() = FBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        leadersDBAdapter.findBySport(SiteRoute.FOOTBALL)?.categories?.associate {
            it.displayName to it.leaders.mapNotNull { leader ->
                athletes[leader.athleteId.toString()]?.let { athlete ->
                    LeaderResponse(
                        athleteName = athlete.shortName,
                        leaderStat = leader.value,
                        teamAbbr = athlete.teamAbbr
                    )
                }
            }
        }
    }

    suspend fun getLeaderCategories() = leadersDBAdapter.findBySport(SiteRoute.FOOTBALL)?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = FBallState.rosters.find { it.teamId == teamId }?.toResponse()
    fun getTeamSchedule(teamId: String) = FBallState.schedules.find { it.teamId == teamId }?.toResponse()
}

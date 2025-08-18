package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.toResponse

class BBallApi(
    private val teamsDBAdapter: TeamsDBAdapter,
    private val standingsDBAdapter: StandingsDBAdapter,
    private val leadersDBAdapter: LeadersDBAdapter
) {
    suspend fun getTeamByAbbr(teamAbbr: String) = teamsDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamAbbr")?.toResponse()
    suspend fun getTeams() = teamsDBAdapter.findBySport(SiteRoute.BASKETBALL).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = standingsDBAdapter.findById(SiteRoute.BASKETBALL.toString())?.standingsGroups?.associate { standingsGroup ->
        standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
    }
    suspend fun getLeaders() = BBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        leadersDBAdapter.findBySport(SiteRoute.BASKETBALL)?.categories?.associate {
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

    suspend fun getLeaderCategories() = leadersDBAdapter.findBySport(SiteRoute.BASKETBALL)?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = BBallState.rosters.find { it.teamId == teamId }?.toResponse()
    fun getTeamSchedule(teamId: String) = BBallState.schedules.find { it.teamId == teamId }?.toResponse()
}

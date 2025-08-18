package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.toResponse


class FBallApi(
    private val standingsDBAdapter: StandingsDBAdapter
) {
    fun getTeamByAbbr(teamAbbr: String) = FBallState.teams.find { it.abbreviation == teamAbbr }?.toResponse()
    fun getTeams() = FBallState.teams.map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = standingsDBAdapter.findById(SiteRoute.FOOTBALL.toString())?.standingsGroups?.flatMap { confGroup ->
        confGroup.subGroups
    }?.associate { standingsSubGroup ->
        standingsSubGroup.divName to standingsSubGroup.standings.map { it.toResponse() }
    }
    fun getLeaders() = FBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        FBallState.leaders?.categories?.associate {
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

    fun getLeaderCategories() = FBallState.leaders?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = FBallState.rosters.find { it.team.id == teamId }?.toResponse()
    fun getTeamSchedule(teamId: String) = FBallState.schedules.find { it.team.id == teamId }?.toResponse()
}

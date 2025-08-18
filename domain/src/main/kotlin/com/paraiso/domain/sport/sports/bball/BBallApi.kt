package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.toResponse

class BBallApi {
    fun getTeamByAbbr(teamAbbr: String) = BBallState.teams.find { it.abbreviation == teamAbbr }?.toResponse()
    fun getTeams() = BBallState.teams.map { it.toResponse() }.associateBy { it.id }
    fun getStandings() = BBallState.standings?.standingsGroups?.associate { standingsGroup ->
        standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
    }
    fun getLeaders() = BBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        BBallState.leaders?.categories?.associate {
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

    fun getLeaderCategories() = BBallState.leaders?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = BBallState.rosters.find { it.team.id == teamId }?.toResponse()
    fun getTeamSchedule(teamId: String) = BBallState.schedules.find { it.team.id == teamId }?.toResponse()
}

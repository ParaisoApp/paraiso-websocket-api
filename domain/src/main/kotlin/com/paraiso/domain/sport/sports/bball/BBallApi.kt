package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.sport.data.LeaderReturn

class BBallApi {
    fun getTeamByAbbr(teamAbbr: String) = BBallState.teams.find { it.abbreviation == teamAbbr }
    fun getTeams() = BBallState.teams.associateBy { it.id }
    fun getStandings() = BBallState.standings?.standingsGroups?.associate { it.confAbbr to it.standings }
    fun getLeaders() = BBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        BBallState.leaders?.categories?.associate {
            it.displayName to it.leaders.mapNotNull { leader ->
                athletes[leader.athleteId.toString()]?.let { athlete ->
                    LeaderReturn(
                        athleteName = athlete.shortName,
                        leaderStat = leader.value,
                        teamAbbr = athlete.teamAbbr
                    )
                }
            }
        }
    }

    fun getLeaderCategories() = BBallState.leaders?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = BBallState.rosters.find { it.team.id == teamId }
    fun getTeamSchedule(teamId: String) = BBallState.schedules.find { it.team.id == teamId }
}

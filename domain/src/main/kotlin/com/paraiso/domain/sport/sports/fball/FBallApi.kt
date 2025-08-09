package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.sport.data.LeaderReturn

class FBallApi {
    fun getTeamByAbbr(teamAbbr: String) = FBallState.teams.find { it.abbreviation == teamAbbr }
    fun getTeams() = FBallState.teams.associateBy { it.id }
    fun getStandings() = FBallState.standings?.standingsGroups?.flatMap { confGroup ->
        confGroup.subGroups
    }?.associate { it.divName to it.standings }
    fun getLeaders() = FBallState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        FBallState.leaders?.categories?.associate {
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

    fun getLeaderCategories() = FBallState.leaders?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = FBallState.rosters.find { it.team.id == teamId }
    fun getTeamSchedule(teamId: String) = FBallState.schedules.find { it.team.id == teamId }
}

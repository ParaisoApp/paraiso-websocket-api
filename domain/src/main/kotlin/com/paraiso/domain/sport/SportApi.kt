package com.paraiso.domain.sport

import com.paraiso.domain.sport.sports.LeaderReturn


class SportApi {
    fun getTeams() = SportState.teams.associateBy { it.id }
    fun getStandings() = SportState.standings?.standingsGroups?.associate { it.confAbbr to it.standings }
    fun getLeaders() = SportState.rosters.flatMap { it.athletes }.associateBy { it.id }.let { athletes ->
        SportState.leaders?.categories?.associate {
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

    fun getLeaderCategories() = SportState.leaders?.categories?.map { it.displayName }
    fun getTeamRoster(teamId: String) = SportState.rosters.find { it.team.id == teamId }
    fun getTeamSchedule(teamId: String) = SportState.schedules.find { it.team.id == teamId }
}
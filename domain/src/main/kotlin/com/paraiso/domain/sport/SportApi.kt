package com.paraiso.domain.sport


class SportApi {
    fun getTeams() = SportState.teams
    fun getStandings() = SportState.standings
    fun getRosters() = SportState.rosters
    fun getLeaders() = SportState.leaders
    fun getTeamRoster(teamId: String) = SportState.rosters.find { it.team.id == teamId }
    fun getTeamSchedule(teamId: String) = SportState.rosters.find { it.team.id == teamId }
}
package com.paraiso.domain.sport.data

import kotlinx.serialization.Serializable

@Serializable
data class BoxScore(
    val teams: List<FullTeam>
)

@Serializable
data class FullTeam(
    val teamId: String,
    val teamStats: List<TeamStat>,
    val statTypes: StatTypes?,
    val athletes: List<Athlete>?
)

@Serializable
data class TeamStat(
    val displayValue: String,
    val abbreviation: String?,
    val label: String
)

@Serializable
data class StatTypes(
    val name: String?,
    val names: List<String>,
    val descriptions: List<String>
)

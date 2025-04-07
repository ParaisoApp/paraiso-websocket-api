package com.paraiso.websocket.api.messageTypes.sports

import kotlinx.serialization.Serializable

@Serializable
data class BoxScore(
    val teams: List<FullTeam>
)

@Serializable
data class FullTeam(
    val teamId: String,
    val teamStats: List<TeamStat>,
    val statTypes: StatTypes,
    val athletes: List<Athlete>?
)

@Serializable
data class TeamStat(
    val displayValue: String,
    val abbreviation: String,
    val label: String
)

@Serializable
data class StatTypes(
    val names: List<String>,
    val descriptions: List<String>
)

@Serializable
data class Athlete(
    val id: String,
    val displayName: String,
    val shortName: String,
    val jersey: String,
    val positionName: String,
    val positionAbbreviation: String,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String,
    val ejected: Boolean,
    val stats: List<String>
)

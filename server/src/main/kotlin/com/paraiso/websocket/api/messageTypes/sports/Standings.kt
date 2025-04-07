package com.paraiso.websocket.api.messageTypes.sports

import kotlinx.serialization.Serializable

@Serializable
data class AllStandings(
    val standingsGroups: List<StandingsGroup>
)

@Serializable
data class StandingsGroup(
    val confName: String,
    val confAbbr: String,
    val standings: List<Standings>
)

@Serializable
data class Standings(
    val teamId: String,
    val seed: Int,
    val stats: List<StandingsStat>
)

@Serializable
data class StandingsStat(
    val shortDisplayName: String,
    val displayValue: String,
    val displayName: String,
    val value: Double
)

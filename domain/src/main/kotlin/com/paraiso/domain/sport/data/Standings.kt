package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllStandings(
    val standingsGroups: List<StandingsGroup>
)

@Serializable
data class StandingsGroup(
    val confName: String,
    val confAbbr: String,
    val standings: List<Standings>,
    val subGroups: List<StandingsSubGroup>
)

@Serializable
data class StandingsSubGroup(
    val divName: String,
    val divAbbr: String,
    val standings: List<Standings>
)

@Serializable
data class Standings(
    val id: String,
    val seed: Int?,
    val stats: List<StandingsStat>
)

@Serializable
data class StandingsStat(
    val shortDisplayName: String,
    val displayValue: String,
    val displayName: String,
    val value: Double?
)

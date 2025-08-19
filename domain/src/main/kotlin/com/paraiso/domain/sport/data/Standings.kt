package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllStandings(
    @SerialName(ID) val id: String,
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
    val teamId: String,
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

@Serializable
data class StandingsResponse(
    val teamId: String,
    val seed: Int?,
    val stats: List<StandingsStatResponse>
)

@Serializable
data class StandingsStatResponse(
    val shortDisplayName: String,
    val displayValue: String,
    val displayName: String,
    val value: Double?
)

fun Standings.toResponse() =
    StandingsResponse(
        teamId = teamId,
        seed = seed,
        stats = stats.map { it.toResponse() },
    )

fun StandingsStat.toResponse() =
    StandingsStatResponse(
        shortDisplayName = shortDisplayName,
        displayValue = displayValue,
        displayName = displayName,
        value = value,
    )

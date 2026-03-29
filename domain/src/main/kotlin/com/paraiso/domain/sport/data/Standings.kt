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
    val shortDisplayName: String?,
    val displayValue: String?,
    val displayName: String?,
    val value: Double?
)

@Serializable
data class StandingsResponse(
    val teamId: String,
    val team: Team?,
    val seed: Int,
    val confName: String,
    val confAbbr: String,
    val divName: String?,
    val divAbbr: String?,
    val stats: Map<String, StandingsStatResponse>
)

@Serializable
data class StandingsStatResponse(
    val shortDisplayName: String?,
    val displayValue: String?,
    val displayName: String?,
    val value: Double?
)

fun Standings.toResponse(
    team: Team?,
    confName: String,
    confAbbr: String,
    divName: String?,
    divAbbr: String?
): StandingsResponse {
    val mappedResponse = stats.associate {
        (it.shortDisplayName ?: "") to it.toResponse()
    }.toMutableMap()
    // to build table rows (for team icon and team abbreviation)
    mappedResponse["abbr"] = StandingsStatResponse(
        shortDisplayName = "abbr",
        displayValue = team?.abbreviation,
        displayName = null,
        value = null
    )
    mappedResponse["id"] = StandingsStatResponse(
        shortDisplayName = "id",
        displayValue = team?.abbreviation,
        displayName = null,
        value = null
    )
    return StandingsResponse(
        teamId = teamId,
        team = team,
        seed = mappedResponse["POS"]?.displayValue?.toIntOrNull() ?: 0,
        confName = confName,
        confAbbr = confAbbr,
        divName = divName,
        divAbbr = divAbbr,
        stats = mappedResponse
    )
}

fun StandingsStat.toResponse() =
    StandingsStatResponse(
        shortDisplayName = shortDisplayName,
        displayValue = displayValue,
        displayName = displayName,
        value = value
    )

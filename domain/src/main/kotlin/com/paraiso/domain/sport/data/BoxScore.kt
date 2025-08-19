package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoxScore(
    @SerialName(ID) val id: String,
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

@Serializable
data class BoxScoreResponse(
    val teams: List<FullTeamResponse>
)

@Serializable
data class FullTeamResponse(
    val teamId: String,
    val teamStats: List<TeamStatResponse>,
    val statTypes: StatTypesResponse?,
    val athletes: List<AthleteResponse>?
)

@Serializable
data class TeamStatResponse(
    val displayValue: String,
    val abbreviation: String?,
    val label: String
)

@Serializable
data class StatTypesResponse(
    val name: String?,
    val names: List<String>,
    val descriptions: List<String>
)

fun BoxScore.toResponse() =
    BoxScoreResponse(
        teams = teams.map { it.toResponse() }
    )

fun FullTeam.toResponse() =
    FullTeamResponse(
        teamId = teamId,
        teamStats = teamStats.map { it.toResponse() },
        statTypes = statTypes?.toResponse(),
        athletes = athletes?.map { it.toResponse() }
    )

fun TeamStat.toResponse() =
    TeamStatResponse(
        displayValue = displayValue,
        abbreviation = abbreviation,
        label = label
    )

fun StatTypes.toResponse() =
    StatTypesResponse(
        name = name,
        names = names,
        descriptions = descriptions
    )

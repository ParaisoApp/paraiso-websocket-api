package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.data.FullTeam as FullTeamDomain
import com.paraiso.domain.sport.data.TeamStat as TeamStatDomain
import com.paraiso.domain.sport.data.StatTypes as StatTypesDomain

@Serializable
data class BoxScore(
    @SerialName(ID) val id: String,
    val teams: List<FullTeam>,
    val completed: Boolean? = null
)

@Serializable
data class FullTeam(
    val teamId: String,
    val teamStats: List<TeamStat>,
    val statTypes: List<StatTypes>
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
    val labels: List<String>,
    val descriptions: List<String>,
    val athletes: List<Athlete>
)

fun BoxScoreDomain.toEntity() =
    BoxScore(
        id = id,
        teams = teams.map { it.toEntity() }
    )

fun FullTeamDomain.toEntity() =
    FullTeam(
        teamId = teamId,
        teamStats = teamStats.map { it.toEntity() },
        statTypes = statTypes.map { it.toEntity() }
    )

fun TeamStatDomain.toEntity() =
    TeamStat(
        displayValue = displayValue,
        abbreviation = abbreviation,
        label = label
    )

fun StatTypesDomain.toEntity() =
    StatTypes(
        name = name,
        labels = labels,
        descriptions = descriptions,
        athletes = athletes.map { it.toEntity() }
    )

fun BoxScore.toDomain() =
    BoxScoreDomain(
        id = id,
        teams = teams.map { it.toDomain() }
    )

fun FullTeam.toDomain() =
    FullTeamDomain(
        teamId = teamId,
        teamStats = teamStats.map { it.toDomain() },
        statTypes = statTypes.map { it.toDomain() }
    )

fun TeamStat.toDomain() =
    TeamStatDomain(
        displayValue = displayValue,
        abbreviation = abbreviation,
        label = label
    )

fun StatTypes.toDomain() =
    StatTypesDomain(
        name = name,
        labels = labels,
        descriptions = descriptions,
        athletes = athletes.map { it.toDomain() }
    )

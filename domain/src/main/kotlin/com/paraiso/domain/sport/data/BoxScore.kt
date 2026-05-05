package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoxScore(
    val id: String,
    val teams: List<FullTeam>,
    val completed: Boolean? = null,
    val createdOn: Instant?,
    val updatedOn: Instant?
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

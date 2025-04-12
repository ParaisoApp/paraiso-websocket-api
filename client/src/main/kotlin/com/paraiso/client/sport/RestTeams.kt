package com.paraiso.client.sport

import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.sports.Team as TeamDomain

@Serializable
data class RestTeams(
    val sports: List<Sport>
)

@Serializable
data class Sport(
    val leagues: List<League>
)

@Serializable
data class League(
    val teams: List<TeamContainer>
)

@Serializable
data class TeamContainer(
    val team: RestTeam
)

@Serializable
data class RestTeam(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String? = null,
    val seed: String? = null
)

fun RestTeams.toDomain(): List<TeamDomain> = sports.first().leagues.first().teams.map { it.team.toDomain() }

fun RestTeam.toDomain(): TeamDomain = TeamDomain(
    id = id,
    location = location,
    name = name,
    abbreviation = abbreviation,
    displayName = displayName,
    shortDisplayName = shortDisplayName ?: ""
)

package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Team as TeamDomain

@Serializable
data class RestTeams(
    val sports: List<RestSport>
)

@Serializable
data class RestSport(
    val leagues: List<RestLeagueTeams>
)

@Serializable
data class RestLeagueTeams(
    val teams: List<RestTeamContainer>
)

@Serializable
data class RestTeamContainer(
    val team: RestTeam
)

@Serializable
data class RestTeam(
    val id: String,
    val location: String,
    val name: String? = null,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String? = null,
    val seed: String? = null
)

fun RestTeams.toDomain(): List<TeamDomain> = sports.first().leagues.first().teams.map { it.team.toDomain() }

fun RestTeam.toDomain(): TeamDomain = TeamDomain(
    id = null,
    teamId = id,
    location = location,
    name = name,
    abbreviation = abbreviation,
    displayName = displayName,
    shortDisplayName = shortDisplayName
)

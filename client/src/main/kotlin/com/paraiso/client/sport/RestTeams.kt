package com.paraiso.client.sport

import com.paraiso.domain.routes.SiteRoute
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Team as TeamDomain

@Serializable
data class RestTeams(
    val sports: List<Sport>
)

@Serializable
data class Sport(
    val leagues: List<RestLeagueTeams>
)

@Serializable
data class RestLeagueTeams(
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
    val name: String? = null,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String? = null,
    val seed: String? = null
)

fun RestTeams.toDomain(sport: SiteRoute): List<TeamDomain> = sports.first().leagues.first().teams.map { it.team.toDomain(sport) }

fun RestTeam.toDomain(sport: SiteRoute): TeamDomain = TeamDomain(
    id = "$sport-$abbreviation",
    sport = sport,
    teamId = id,
    location = location,
    name = name,
    abbreviation = abbreviation,
    displayName = displayName,
    shortDisplayName = shortDisplayName
)

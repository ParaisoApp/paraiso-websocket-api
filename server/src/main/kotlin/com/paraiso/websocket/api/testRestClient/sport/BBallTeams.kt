package com.paraiso.websocket.api.testRestClient.sport

import kotlinx.serialization.Serializable
import com.paraiso.websocket.api.messageTypes.sports.Team as TeamDomain

@Serializable
data class BBallTeams(
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
    val team: Team
)

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String,
    val seed: String? = null
)

fun BBallTeams.toDomain(): List<TeamDomain> = sports.first().leagues.first().teams.map { it.team.toDomain() }

fun Team.toDomain(): TeamDomain = TeamDomain(
    id = id,
    location = location,
    name = name,
    abbreviation = abbreviation,
    displayName = displayName,
    shortDisplayName = shortDisplayName
)

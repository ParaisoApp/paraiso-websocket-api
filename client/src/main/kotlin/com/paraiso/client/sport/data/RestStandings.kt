package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.data.Standings as StandingsDomain
import com.paraiso.domain.sport.data.StandingsGroup as StandingsGroupDomain
import com.paraiso.domain.sport.data.StandingsStat as StandingsStatDomain
import com.paraiso.domain.sport.data.StandingsSubGroup as StandingsSubGroupDomain

@Serializable
data class RestStandingsContainer(
    val content: RestStandingsContent
)

@Serializable
data class RestStandingsContent(
    val standings: RestStandings
)

@Serializable
data class RestStandings(
    val groups: List<RestGroup>
)

@Serializable
data class RestGroup(
    val name: String,
    val abbreviation: String,
    val standings: RestStandingsEntries? = null,
    val groups: List<RestSubGroup>? = null
)

@Serializable
data class RestSubGroup(
    val name: String,
    val abbreviation: String,
    val standings: RestStandingsEntries
)

@Serializable
data class RestStandingsEntries(
    val entries: List<RestEntry>
)

@Serializable
data class RestEntry(
    val team: RestTeam,
    val stats: List<RestBBallStandingsStat>
)

@Serializable
data class RestBBallStandingsStat(
    val shortDisplayName: String,
    val displayValue: String,
    val displayName: String,
    val value: String
)

fun RestStandingsContainer.toDomain(sport: SiteRoute) = AllStandingsDomain(
    id = sport.toString(),
    standingsGroups = content.standings.groups.map { it.toDomain() }
)

fun RestGroup.toDomain() = StandingsGroupDomain(
    confName = name,
    confAbbr = abbreviation,
    standings = standings?.entries?.map { it.toDomain() } ?: emptyList(),
    subGroups = groups?.map { it.toDomain() } ?: emptyList()
)

fun RestSubGroup.toDomain() = StandingsSubGroupDomain(
    divName = name,
    divAbbr = abbreviation,
    standings = standings.entries.map { it.toDomain() }
)

fun RestEntry.toDomain() = StandingsDomain(
    teamId = team.id,
    seed = team.seed?.toIntOrNull(),
    stats = stats.map { it.toDomain() }
)

fun RestBBallStandingsStat.toDomain() = StandingsStatDomain(
    shortDisplayName = shortDisplayName,
    displayValue = displayValue,
    displayName = displayName,
    value = value.toDoubleOrNull()
)

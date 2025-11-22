package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.data.Standings as StandingsDomain
import com.paraiso.domain.sport.data.StandingsGroup as StandingsGroupDomain
import com.paraiso.domain.sport.data.StandingsStat as StandingsStatDomain
import com.paraiso.domain.sport.data.StandingsSubGroup as StandingsSubGroupDomain

@Serializable
data class RestStandings(
    val children: List<RestGroup>
)

@Serializable
data class RestGroup(
    val name: String,
    val abbreviation: String,
    val standings: RestStandingsEntries? = null,
    val children: List<RestSubGroup>? = null
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
    val stats: List<RestStandingsStat>
)

@Serializable
data class RestStandingsStat(
    val shortDisplayName: String? = null,
    val displayValue: String? = null,
    val displayName: String? = null,
    val value: String? = null
)

fun RestStandings.toDomain(sport: SiteRoute) = AllStandingsDomain(
    id = sport.toString(),
    standingsGroups = children.map { it.toDomain() }
)

fun RestGroup.toDomain() = StandingsGroupDomain(
    confName = name,
    confAbbr = abbreviation,
    standings = standings?.entries?.map { it.toDomain() } ?: emptyList(),
    subGroups = children?.map { it.toDomain() } ?: emptyList()
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

fun RestStandingsStat.toDomain() = StandingsStatDomain(
    shortDisplayName = shortDisplayName,
    displayValue = displayValue,
    displayName = displayName,
    value = value?.toDoubleOrNull()
)

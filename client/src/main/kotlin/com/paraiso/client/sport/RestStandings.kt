package com.paraiso.client.sport

import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.sports.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.sports.Standings as StandingsDomain
import com.paraiso.domain.sport.sports.StandingsGroup as StandingsGroupDomain
import com.paraiso.domain.sport.sports.StandingsStat as StandingsStatDomain

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
    val groups: List<Group>
)

@Serializable
data class Group(
    val name: String,
    val abbreviation: String,
    val standings: Standings
)

@Serializable
data class Standings(
    val entries: List<Entry>
)

@Serializable
data class Entry(
    val team: Team,
    val stats: List<BBallStandingsStat>
)

@Serializable
data class BBallStandingsStat(
    val shortDisplayName: String,
    val displayValue: String,
    val displayName: String,
    val value: String
)

fun RestStandingsContainer.toDomain() = AllStandingsDomain(
    standingsGroups = content.standings.groups.map { it.toDomain() }
)

fun Group.toDomain() = StandingsGroupDomain(
    confName = name,
    confAbbr = abbreviation,
    standings = standings.entries.map { it.toDomain() }
)

fun Entry.toDomain() = StandingsDomain(
    teamId = team.id,
    seed = team.seed?.toIntOrNull() ?: 0,
    stats = stats.map { it.toDomain() }
)

fun BBallStandingsStat.toDomain() = StandingsStatDomain(
    shortDisplayName = shortDisplayName,
    displayValue = displayValue,
    displayName = displayName,
    value = value.toDoubleOrNull() ?: 0.0
)

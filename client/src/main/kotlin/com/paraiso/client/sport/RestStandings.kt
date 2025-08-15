package com.paraiso.client.sport

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
    val groups: List<Group>
)

@Serializable
data class Group(
    val name: String,
    val abbreviation: String,
    val standings: Standings? = null,
    val groups: List<SubGroup>? = null
)

@Serializable
data class SubGroup(
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
    val team: RestTeam,
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
    standings = standings?.entries?.map { it.toDomain() } ?: emptyList(),
    subGroups = groups?.map { it.toDomain() } ?: emptyList()
)

fun SubGroup.toDomain() = StandingsSubGroupDomain(
    divName = name,
    divAbbr = abbreviation,
    standings = standings.entries.map { it.toDomain() }
)

fun Entry.toDomain() = StandingsDomain(
    teamId = team.id,
    seed = team.seed?.toIntOrNull(),
    stats = stats.map { it.toDomain() }
)

fun BBallStandingsStat.toDomain() = StandingsStatDomain(
    shortDisplayName = shortDisplayName,
    displayValue = displayValue,
    displayName = displayName,
    value = value.toDoubleOrNull()
)

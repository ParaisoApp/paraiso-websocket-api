package com.example.testRestClient.sport

import kotlinx.serialization.Serializable
import com.example.messageTypes.sports.AllStandings as AllStandingsDomain
import com.example.messageTypes.sports.StandingsGroup as StandingsGroupDomain
import com.example.messageTypes.sports.Standings as StandingsDomain
import com.example.messageTypes.sports.StandingsStat as StandingsStatDomain

@Serializable
data class BBallStandingsContainer(
    val content: BBallStandingsContent
)

@Serializable
data class BBallStandingsContent(
    val standings: BBallStandings
)

@Serializable
data class BBallStandings(
    val groups: List<Group>,
)

@Serializable
data class Group(
    val name: String,
    val abbreviation: String,
    val standings: Standings,
)

@Serializable
data class Standings(
    val entries: List<Entry>,
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

fun BBallStandingsContainer.toDomain() = AllStandingsDomain(
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
    value = value.toDoubleOrNull() ?: 0.0,
)

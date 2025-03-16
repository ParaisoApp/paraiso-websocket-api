package com.example.testRestClient.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.messageTypes.sports.AllStandings as AllStandingsDomain
import com.example.messageTypes.sports.Standings as StandingsDomain
import com.example.messageTypes.sports.RecordTypes as RecordTypesDomain
import com.example.messageTypes.sports.RecordStat as RecordStatDomain

@Serializable
data class BBallStandings(
    val standings: List<Standings>
)

@Serializable
data class Standings(
    val team: TeamRef,
    val records: List<RecordTypes>
)

@Serializable
data class TeamRef(
    @SerialName("\$ref") val ref: String
)

@Serializable
data class RecordTypes(
    val displayName: String,
    val displayValue: String,
    val value: Double,
    val stats: List<RecordStat>
)

@Serializable
data class RecordStat(
    val displayName: String,
    val abbreviation: String,
    val value: Double
)

fun BBallStandings.toDomain() = AllStandingsDomain(
    standings = standings.map { it.toDomain() }
)

fun Standings.toDomain() = StandingsDomain(
    teamId = team.ref.substringAfter("/teams/").substringBefore("?"),
    records = records.map { it.toDomain() }
)

fun RecordTypes.toDomain() = RecordTypesDomain(
    displayName = displayName,
    displayValue = displayValue,
    stats = stats.map { it.toDomain() }
)

fun RecordStat.toDomain() = RecordStatDomain(
    displayName = displayName,
    abbreviation = abbreviation,
    value = value
)

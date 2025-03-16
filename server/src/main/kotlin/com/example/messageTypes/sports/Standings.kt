package com.example.messageTypes.sports

import kotlinx.serialization.Serializable

@Serializable
data class AllStandings(
    val standings: List<Standings>
)

@Serializable
data class Standings(
    val teamId: String,
    val records: List<RecordTypes>
)

@Serializable
data class RecordTypes(
    val displayName: String,
    val wins: Int,
    val losses: Int,
    val stats: List<RecordStat>
)

@Serializable
data class RecordStat(
    val displayName: String,
    val abbreviation: String,
    val value: Double
)

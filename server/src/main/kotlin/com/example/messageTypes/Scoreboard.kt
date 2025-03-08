package com.example.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Scoreboard(
    val competitions: List<Competition>
)

@Serializable
data class Competition(
    val id: String,
    val teams: List<Team>,
    val venue: Venue,
    val status: Status
)

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String,
    val homeAway: String,
    val records: List<Record>,
    val winner: Boolean? = false,
    val linescores: List<LineScore>? = null,
    val score: String
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String
)

@Serializable
data class Status(
    val clock: Double,
    val period: Int,
    val name: String,
    val state: String,
    val completed: Boolean
)

@Serializable
data class LineScore(
    val value: Double
)

@Serializable
data class Record(
    val name: String,
    val summary: String,
)
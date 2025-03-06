package com.example.testRestClient.sport

import kotlinx.serialization.Serializable

@Serializable
data class BBallScoreboard(
    val events: List<Event>
)

@Serializable
data class Event(
    val name: String,
    val shortName: String,
    val competitions: List<Competition>,
)

@Serializable
data class Competition(
    val id: String,
    val venue: Venue,
    val competitors: List<Competitor>,
    val status: Status
)

@Serializable
data class Competitor(
    val homeAway: String,
    val winner: Boolean? = false,
    val team: Team,
    val score: String,
    val linescores: List<LineScore>,
    val records: List<Record>
)

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String,
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

@Serializable
data class Venue(
    val fullName: String,
    val address: Address
)

@Serializable
data class Address(
    val city: String,
    val state: String
)

@Serializable
data class Status(
    val clock: Double,
    val period: Int,
    val type: Type
)

@Serializable
data class Type(
    val name: String,
    val state: String,
    val completed: Boolean
)

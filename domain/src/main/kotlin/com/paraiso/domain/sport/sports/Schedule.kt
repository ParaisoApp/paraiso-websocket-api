package com.paraiso.domain.sport.sports

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val team: Team,
    val events: List<Competition>
)

@Serializable
data class Competition(
    val id: String,
    val name: String,
    val shortName: String,
    val date: String,
    val teams: List<TeamGameStats>,
    val venue: Venue,
    val status: Status
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String
)

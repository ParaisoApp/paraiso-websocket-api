package com.paraiso.domain.sport.sports

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val team: Team,
    val events: List<Event>
)

@Serializable
data class Event(
    val id: String,
    val name: String,
    val shortName: String,
    val date: String,
    val competitions: List<Competition>
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

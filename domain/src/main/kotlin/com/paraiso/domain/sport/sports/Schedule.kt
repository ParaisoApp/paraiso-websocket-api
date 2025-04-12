package com.paraiso.domain.sport.sports

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val team: Team,
    val events: List<Event>
)

@Serializable
data class Event(
    val name: String,
    val shortName: String,
    val date: String,
    val competitions: List<Competition>
)

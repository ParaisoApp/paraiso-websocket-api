package com.paraiso.client.sport

import kotlinx.serialization.Serializable

@Serializable
data class RestSchedule(
    val season: RestSeason,
    val team: RestTeam,
    val events: List<RestEvent>
)

@Serializable
data class RestEvent(
    val name: String,
    val shortName: String,
    val date: String,
    val competitions: List<RestCompetition>
)

@Serializable
data class RestCompetition(
    val id: String,
    val venue: Venue,
    val competitors: List<RestCompetitor>,
    val status: Status
)

@Serializable
data class RestCompetitor(
    val homeAway: String,
    val team: RestTeam,
    val winner: Boolean? = null,
    val score: RestScore,
    val statistics: List<TeamYearStats>,
    val linescores: List<LineScore>? = null,
    val records: List<Record>
)
@Serializable
data class RestScore(
    val value: Double,
    val displayValue: String
)

@Serializable
data class RestSeason(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
)

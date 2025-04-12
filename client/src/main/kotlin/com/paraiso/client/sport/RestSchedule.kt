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
    val competitions: List<Competition>
)
@Serializable
data class RestSeason(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
)

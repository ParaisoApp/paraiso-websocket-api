package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    @SerialName(ID) val id: String,
    val team: Team,
    val events: List<Competition>
)

@Serializable
data class Competition(
    @SerialName(ID) val id: String,
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
    val state: String?
)

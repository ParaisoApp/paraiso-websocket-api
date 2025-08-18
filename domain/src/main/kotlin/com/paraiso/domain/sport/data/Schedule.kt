package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    @SerialName(ID) val id: String,
    val teamId: String,
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
data class Status(
    val clock: String,
    val period: Int,
    val name: String,
    val state: String,
    val completed: Boolean
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String?
)

@Serializable
data class ScheduleResponse(
    val id: String,
    val teamId: String,
    val events: List<CompetitionResponse>
)

@Serializable
data class CompetitionResponse(
    val id: String,
    val name: String,
    val shortName: String,
    val date: String,
    val teams: List<TeamGameStatsResponse>,
    val venue: VenueResponse,
    val status: StatusResponse
)

@Serializable
data class StatusResponse(
    val clock: String,
    val period: Int,
    val name: String,
    val state: String,
    val completed: Boolean
)

@Serializable
data class VenueResponse(
    val fullName: String,
    val city: String,
    val state: String?
)

fun Schedule.toResponse() =
    ScheduleResponse(
        id = id,
        teamId = teamId,
        events = events.map { it.toResponse() },
    )

fun Competition.toResponse() =
    CompetitionResponse(
        id = id,
        name = name,
        shortName = shortName,
        date = date,
        teams = teams.map { it.toResponse() },
        venue = venue.toResponse(),
        status = status.toResponse(),
    )

fun Status.toResponse() =
    StatusResponse(
        clock = clock,
        period = period,
        name = name,
        state = state,
        completed = completed,
    )

fun Venue.toResponse() =
    VenueResponse(
        fullName = fullName,
        city = city,
        state = state,
    )

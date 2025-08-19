package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    @SerialName(ID) val id: String,
    val season: Season,
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
data class Season(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
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
    val season: SeasonResponse,
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
data class SeasonResponse(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
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

@Serializable
data class ScheduleEntity(
    @SerialName(ID) val id: String,
    val season: SeasonEntity,
    val teamId: String,
    val events: List<String>
)

@Serializable
data class SeasonEntity(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
)

fun Schedule.toResponse() =
    ScheduleResponse(
        id = id,
        season = season.toResponse(),
        teamId = teamId,
        events = events.map { it.toResponse() }
    )

fun Competition.toResponse() =
    CompetitionResponse(
        id = id,
        name = name,
        shortName = shortName,
        date = date,
        teams = teams.map { it.toResponse() },
        venue = venue.toResponse(),
        status = status.toResponse()
    )

fun Season.toResponse() =
    SeasonResponse(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

fun Status.toResponse() =
    StatusResponse(
        clock = clock,
        period = period,
        name = name,
        state = state,
        completed = completed
    )

fun Venue.toResponse() =
    VenueResponse(
        fullName = fullName,
        city = city,
        state = state
    )

fun Schedule.toEntity() =
    ScheduleEntity(
        id = id,
        season = season.toEntity(),
        teamId = teamId,
        events = events.map { it.id }
    )

fun Season.toEntity() =
    SeasonEntity(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

fun ScheduleEntity.toDomain(events: List<Competition>) =
    Schedule(
        id = id,
        season = season.toDomain(),
        teamId = teamId,
        events = events
    )

fun SeasonEntity.toDomain() =
    Season(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

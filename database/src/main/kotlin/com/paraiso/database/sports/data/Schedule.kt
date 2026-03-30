package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.data.Competition as CompetitionDomain
import com.paraiso.domain.sport.data.Season as SeasonDomain
import com.paraiso.domain.sport.data.Situation as SituationDomain
import com.paraiso.domain.sport.data.Status as StatusDomain
import com.paraiso.domain.sport.data.Venue as VenueDomain

@Serializable
data class Schedule(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val season: Season,
    val teamId: String,
    val events: List<Competition>
)

@Serializable
data class Competition(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val name: String,
    val shortName: String,
    @Serializable(with = InstantBsonSerializer::class)
    val date: Instant?,
    val week: Int?,
    val season: Season?,
    val teams: List<TeamGameStats>,
    val venue: Venue,
    val situation: Situation?,
    val status: Status
)

@Serializable
data class Season(
    val year: Int,
    val type: Int,
    val name: String?,
    val displayName: String?
)

@Serializable
data class Situation(
    val down: Int? = null,
    val distance: Int? = null,
    val downDistanceText: String? = null,
    val isRedZone: Boolean? = null,
    val homeTimeouts: Int? = null,
    val awayTimeouts: Int? = null,
    val possession: String? = null
)

@Serializable
data class Status(
    val clock: String,
    val period: Int,
    val name: String,
    val state: String,
    val completed: Boolean,
    @Serializable(with = InstantBsonSerializer::class)
    val completedTime: Instant?
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String?
)

fun ScheduleDomain.toEntity() =
    Schedule(
        id = id,
        sport = sport,
        season = season.toEntity(),
        teamId = teamId,
        events = events.map { it.toEntity() }
    )

fun CompetitionDomain.toEntity() =
    Competition(
        id = id,
        sport = sport,
        name = name,
        shortName = shortName,
        date = date,
        week = week,
        season = season?.toEntity(),
        teams = teams.map { it.toEntity() },
        venue = venue.toEntity(),
        situation = situation?.toEntity(),
        status = status.toEntity()
    )

fun SeasonDomain.toEntity() =
    Season(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

fun SituationDomain.toEntity() = Situation(
    down = down,
    distance = distance,
    downDistanceText = downDistanceText,
    isRedZone = isRedZone,
    homeTimeouts = homeTimeouts,
    awayTimeouts = awayTimeouts,
    possession = possession
)

fun StatusDomain.toEntity() =
    Status(
        clock = clock,
        period = period,
        name = name,
        state = state,
        completed = completed,
        completedTime = completedTime
    )

fun VenueDomain.toEntity() =
    Venue(
        fullName = fullName,
        city = city,
        state = state
    )

fun Schedule.toDomain() =
    ScheduleDomain(
        id = id,
        sport = sport,
        season = season.toDomain(),
        teamId = teamId,
        events = events.map { it.toDomain() }
    )

fun Competition.toDomain() =
    CompetitionDomain(
        id = id,
        sport = sport,
        name = name,
        shortName = shortName,
        date = date,
        week = week,
        season = season?.toDomain(),
        teams = teams.map { it.toDomain() },
        venue = venue.toDomain(),
        situation = situation?.toDomain(),
        status = status.toDomain()
    )

fun Season.toDomain() =
    SeasonDomain(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

fun Situation.toDomain() = SituationDomain(
    down = down,
    distance = distance,
    downDistanceText = downDistanceText,
    isRedZone = isRedZone,
    homeTimeouts = homeTimeouts,
    awayTimeouts = awayTimeouts,
    possession = possession
)

fun Status.toDomain() =
    StatusDomain(
        clock = clock,
        period = period,
        name = name,
        state = state,
        completed = completed,
        completedTime = completedTime
    )

fun Venue.toDomain() =
    VenueDomain(
        fullName = fullName,
        city = city,
        state = state
    )

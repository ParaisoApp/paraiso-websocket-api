package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.data.Season as SeasonDomain

@Serializable
data class Schedule(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val season: Season,
    val teamId: String,
    val events: List<String>
)

@Serializable
data class Season(
    val year: Int,
    val type: Int,
    val name: String?,
    val displayName: String?
)

fun ScheduleDomain.toEntity() =
    Schedule(
        id = id,
        sport = sport,
        season = season.toEntity(),
        teamId = teamId,
        events = events.map { it.id }
    )

fun SeasonDomain.toEntity() =
    Season(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

fun Schedule.toDomain() =
    ScheduleDomain(
        id = id,
        sport = sport,
        season = season.toDomain(),
        teamId = teamId,
        events = emptyList()
    )

fun Season.toDomain() =
    SeasonDomain(
        year = year,
        type = type,
        name = name,
        displayName = displayName
    )

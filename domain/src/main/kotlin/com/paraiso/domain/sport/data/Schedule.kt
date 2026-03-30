package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String,
    val sport: SiteRoute,
    val season: Season,
    val teamId: String,
    val events: List<Competition>
)

@Serializable
data class Competition(
    val id: String,
    val sport: SiteRoute,
    val name: String,
    val shortName: String,
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
    val completedTime: Instant?
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String?
)

fun ScoreboardBasic.toFullData(competitions: List<Competition>) = Scoreboard(
    id = id,
    sport = sport,
    season = season,
    week = week,
    day = day,
    competitions = competitions
)

package com.paraiso.domain.sport.data

import com.paraiso.domain.posts.ActiveStatus
import com.paraiso.domain.routes.SiteRoute
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
    val status: Status,
    val activeStatus: ActiveStatus,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

@Serializable
data class TeamGameStats(
    val teamId: String,
    val homeAway: String,
    val records: List<Record>,
    val winner: Boolean,
    val teamYearStats: List<TeamYearStats>,
    val lineScores: List<Double>,
    val score: String?
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

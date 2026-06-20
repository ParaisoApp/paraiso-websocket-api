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
    val broadcasts: List<Broadcast>?,
    val odds: List<Odds>?,
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
    // football
    val down: Int?,
    val distance: Int?,
    val downDistanceText: String?,
    val isRedZone: Boolean?,
    val homeTimeouts: Int?,
    val awayTimeouts: Int?,
    val possession: String?,
    // baseball
    val balls: Int?,
    val strikes: Int?,
    val outs: Int?,
    val onFirst: Boolean?,
    val onSecond: Boolean?,
    val onThird: Boolean?
)

@Serializable
data class Status(
    val clock: String,
    val period: Int,
    val name: String,
    val state: String,
    val shortDetail: String?,
    val completed: Boolean,
    val completedTime: Instant?
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String?
)

@Serializable
data class Broadcast(
    val market: String,
    val names: List<String>
)

@Serializable
data class Odds(
    val displayName: String,
    val shortDisplayName: String,
    val home: OddsDiff?,
    val away: OddsDiff?,
    val total: OddsDiff?
)

@Serializable
data class OddsDiff(
    val openOdds: String,
    val closeOdds: String,
    val openLine: String?,
    val closeLine: String?
)

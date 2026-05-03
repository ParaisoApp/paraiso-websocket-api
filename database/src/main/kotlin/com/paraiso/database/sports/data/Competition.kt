package com.paraiso.database.sports.data

import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Competition as CompetitionDomain
import com.paraiso.domain.sport.data.TeamGameStats as TeamGameStatsDomain
import com.paraiso.domain.sport.data.Situation as SituationDomain
import com.paraiso.domain.sport.data.Status as StatusDomain
import com.paraiso.domain.sport.data.Venue as VenueDomain

@Serializable
data class Competition(
    @SerialName(Constants.ID) val id: String,
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
    val status: Status,
    val activeStatus: PostStatus
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
    @Serializable(with = InstantBsonSerializer::class)
    val completedTime: Instant?
)

@Serializable
data class Venue(
    val fullName: String,
    val city: String,
    val state: String?
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
        status = status.toEntity(),
        activeStatus = activeStatus
    )

fun TeamGameStatsDomain.toEntity() =
    TeamGameStats(
        teamId = teamId,
        homeAway = homeAway,
        records = records.map { it.toEntity() },
        winner = winner,
        teamYearStats = teamYearStats.map { it.toEntity() },
        lineScores = lineScores,
        score = score
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
        status = status.toDomain(),
        activeStatus = activeStatus
    )

fun TeamGameStats.toDomain() =
    TeamGameStatsDomain(
        teamId = teamId,
        homeAway = homeAway,
        records = records.map { it.toDomain() },
        winner = winner,
        teamYearStats = teamYearStats.map { it.toDomain() },
        lineScores = lineScores,
        score = score
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

package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.data.TeamGameStats as TeamGameStatsDomain
import com.paraiso.domain.sport.data.TeamYearStats as TeamYearStatsDomain
import com.paraiso.domain.sport.data.Record as RecordDomain

@Serializable
data class Scoreboard(
    @SerialName(ID) val id: String,
    val sport: String?,
    val season: Season?,
    val week: Int?,
    @Serializable(with = InstantBsonSerializer::class)
    val day: Instant?,
    val competitions: List<String>
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
data class TeamYearStats(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String?
)

@Serializable
data class Record(
    val name: String?,
    val summary: String?
)

fun ScoreboardDomain.toEntity() =
    Scoreboard(
        id = id,
        sport = sport,
        season = season?.toEntity(),
        week = week,
        day = day,
        competitions = competitions.map { it.id }
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

fun RecordDomain.toEntity() =
    Record(
        name = name,
        summary = summary
    )

fun TeamYearStatsDomain.toEntity() =
    TeamYearStats(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue
    )

fun Scoreboard.toDomain() =
    ScoreboardDomain(
        id = id,
        sport = sport,
        season = season?.toDomain(),
        week = week,
        day = day,
        competitions = emptyList()
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

fun Record.toDomain() =
    RecordDomain(
        name = name,
        summary = summary
    )

fun TeamYearStats.toDomain() =
    TeamYearStatsDomain(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue
    )

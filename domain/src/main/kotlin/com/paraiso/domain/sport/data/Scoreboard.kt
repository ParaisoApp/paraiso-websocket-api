package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Scoreboard(
    val id: String,
    val sport: String?,
    val season: Season,
    val week: Int?,
    val day: Instant?,
    val competitions: List<Competition>
) { companion object }

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

@Serializable
data class ScoreboardResponse(
    val season: Season,
    val week: Int?,
    val day: Instant?,
    val competitions: List<CompetitionResponse>
)

@Serializable
data class TeamGameStatsResponse(
    val teamId: String,
    val homeAway: String,
    val records: List<RecordResponse>,
    val winner: Boolean,
    val teamYearStats: List<TeamYearStatsResponse>,
    val lineScores: List<Double>,
    val score: String?
)

@Serializable
data class RecordResponse(
    val name: String?,
    val summary: String?
)

@Serializable
data class TeamYearStatsResponse(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String?
)

@Serializable
data class ScoreboardEntity(
    @SerialName(ID) val id: String,
    val sport: String?,
    val season: Season,
    val week: Int?,
    @Serializable(with = InstantBsonSerializer::class)
    val day: Instant?,
    val competitions: List<String>
)

fun Scoreboard.toResponse() =
    ScoreboardResponse(
        season = season,
        week = week,
        day = day,
        competitions = competitions.map { it.toResponse() }
    )

fun TeamGameStats.toResponse() =
    TeamGameStatsResponse(
        teamId = teamId,
        homeAway = homeAway,
        records = records.map { it.toResponse() },
        winner = winner,
        teamYearStats = teamYearStats.map { it.toResponse() },
        lineScores = lineScores,
        score = score
    )

fun Record.toResponse() =
    RecordResponse(
        name = name,
        summary = summary
    )

fun TeamYearStats.toResponse() =
    TeamYearStatsResponse(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue
    )

fun Scoreboard.toEntity() =
    ScoreboardEntity(
        id = id,
        sport = sport,
        season = season,
        week = week,
        day = day,
        competitions = competitions.map { it.id }
    )

fun ScoreboardEntity.toDomain(
    competitions: List<Competition>
) =
    Scoreboard(
        id = id,
        sport = sport,
        season = season,
        week = week,
        day = day,
        competitions = competitions
    )

fun Scoreboard.Companion.init() =
    Scoreboard(
        id = "UNKNOWN",
        sport = null,
        season = Season(
            year = 1900,
            type = 1,
            name = null,
            displayName = null
        ),
        week = null,
        day = null,
        competitions = emptyList()
    )

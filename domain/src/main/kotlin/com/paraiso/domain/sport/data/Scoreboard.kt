package com.paraiso.domain.sport.data

import kotlinx.serialization.Serializable

@Serializable
data class Scoreboard(
    val competitions: List<Competition>
)

@Serializable
data class TeamGameStats(
    val team: Team,
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
    val competitions: List<CompetitionResponse>
)

@Serializable
data class TeamGameStatsResponse(
    val team: TeamResponse,
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

fun Scoreboard.toResponse() =
    ScoreboardResponse(
        competitions = competitions.map{ it.toResponse() }
    )

fun TeamGameStats.toResponse() =
    TeamGameStatsResponse(
        team = team.toResponse(),
        homeAway = homeAway,
        records = records.map { it.toResponse() },
        winner = winner,
        teamYearStats = teamYearStats.map{ it.toResponse() },
        lineScores = lineScores,
        score = score,
    )

fun Record.toResponse() =
    RecordResponse(
        name = name,
        summary = summary,
    )

fun TeamYearStats.toResponse() =
    TeamYearStatsResponse(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue,
    )

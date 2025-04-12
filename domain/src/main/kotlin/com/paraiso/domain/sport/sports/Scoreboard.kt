package com.paraiso.domain.sport.sports

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
    val score: String
)

@Serializable
data class TeamYearStats(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String
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
data class Record(
    val name: String,
    val summary: String
)

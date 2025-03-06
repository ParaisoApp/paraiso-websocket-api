package com.example.testRestClient.sport

import kotlinx.serialization.Serializable

@Serializable
data class BBallGameStats (
    val boxscore: BoxScore
)

@Serializable
data class BoxScore (
    val players: List<Player>
)

@Serializable
data class Player (
    val team: Team,
    val statistics: List<Statistic>
)

@Serializable
data class Statistic (
    val names: List<String>,
    val descriptions: List<String>,
    val athletes: List<AthleteBase>
)

@Serializable
data class AthleteBase (
    val athlete: Athlete,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String,
    val ejected: Boolean,
    val stats: List<String>,
)

@Serializable
data class Athlete (
    val id: String,
    val displayName: String,
    val shortName: String,
    val jersey: String? = "",
    val position: Position
)

@Serializable
data class Position (
    val name: String,
    val abbreviation: String,
)
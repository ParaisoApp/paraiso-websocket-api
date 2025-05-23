package com.paraiso.domain.sport.sports

import kotlinx.serialization.Serializable

@Serializable
data class Roster(
    val athletes: List<Athlete>,
    val coach: Coach,
    val team: Team
)

@Serializable
data class Athlete(
    val id: String,
    val teamAbbr: String,
    val displayName: String,
    val shortName: String,
    val jersey: String,
    val positionName: String,
    val positionAbbreviation: String,
    val displayWeight: String,
    val displayHeight: String,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String,
    val ejected: Boolean,
    val stats: List<String>
)

@Serializable
data class Coach(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
) { companion object }

fun Coach.Companion.createUnknown() = Coach(
    id = "",
    firstName = "",
    lastName = "",
    experience = 0
)

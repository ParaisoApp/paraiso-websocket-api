package com.paraiso.domain.sport.sports

import com.paraiso.domain.util.Constants
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String
) { companion object }

fun Team.Companion.unknownTeam() = Team(
    id = Constants.UNKNOWN,
    location = Constants.UNKNOWN,
    name = Constants.UNKNOWN,
    abbreviation = Constants.UNKNOWN,
    displayName = Constants.UNKNOWN,
    shortDisplayName = Constants.UNKNOWN,
)

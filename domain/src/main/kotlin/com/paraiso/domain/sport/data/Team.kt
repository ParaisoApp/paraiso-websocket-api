package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    @SerialName(ID) val id: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
)

@Serializable
data class TeamResponse(
    val id: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
)

fun Team.toResponse() =
    TeamResponse(
        id = id,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        shortDisplayName = shortDisplayName
    )

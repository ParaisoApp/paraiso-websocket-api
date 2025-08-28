package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val teamId: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
)

@Serializable
data class TeamResponse(
    val id: String,
    val teamId: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
)

fun Team.toResponse() =
    TeamResponse(
        id = "$sport-$teamId",
        teamId = teamId,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        shortDisplayName = shortDisplayName
    )

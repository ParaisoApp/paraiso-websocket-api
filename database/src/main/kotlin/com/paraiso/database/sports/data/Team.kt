package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Team as TeamDomain
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

fun TeamDomain.toEntity(sport: SiteRoute) =
    Team(
        id = "$sport-$teamId",
        teamId = teamId,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        sport = sport,
        shortDisplayName = shortDisplayName
    )

fun Team.toDomain() =
    TeamDomain(
        id = id,
        teamId = teamId,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        shortDisplayName = shortDisplayName
    )

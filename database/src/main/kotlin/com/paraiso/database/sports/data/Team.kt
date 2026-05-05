package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Team as TeamDomain
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
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
    val shortDisplayName: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

fun TeamDomain.toEntity() =
    Team( // needed to search teams on abbreviation over id (for init page)
        id = "$sport-$abbreviation",
        sport = sport,
        teamId = teamId,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        shortDisplayName = shortDisplayName,
        // fields are set in DB layer during save
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun Team.toDomain() =
    TeamDomain(
        id = "$sport-$teamId",
        sport = sport,
        teamId = teamId,
        location = location,
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        shortDisplayName = shortDisplayName,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

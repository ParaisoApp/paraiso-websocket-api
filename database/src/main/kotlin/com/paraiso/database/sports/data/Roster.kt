package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Roster as RosterDomain

@Serializable
data class Roster(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val athletes: List<String>,
    val coach: String?,
    val teamId: String,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

fun RosterDomain.toEntity() =
    Roster(
        id = id,
        sport = sport,
        athletes = emptyList(),
        coach = null,
        teamId = teamId,
        // fields are set in DB layer during save
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun Roster.toDomain() =
    RosterDomain(
        id = id,
        sport = sport,
        athletes = emptyList(),
        coach = null,
        teamId = teamId,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

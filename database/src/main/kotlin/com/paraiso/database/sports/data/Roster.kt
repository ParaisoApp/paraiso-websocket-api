package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Roster as RosterDomain

@Serializable
data class Roster(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val athletes: List<String>,
    val coach: String?,
    val teamId: String
)

fun RosterDomain.toEntity() =
    Roster(
        id = id,
        sport = sport,
        athletes = emptyList(),
        coach = null,
        teamId = teamId
    )

fun Roster.toDomain() =
    RosterDomain(
        id = id,
        sport = sport,
        athletes = emptyList(),
        coach = null,
        teamId = teamId
    )

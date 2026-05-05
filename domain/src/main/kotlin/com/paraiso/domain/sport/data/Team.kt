package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val sport: SiteRoute,
    val teamId: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

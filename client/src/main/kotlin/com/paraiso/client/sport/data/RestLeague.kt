package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.League
import kotlinx.serialization.Serializable

@Serializable
data class RestLeague (
    val id: String,
    val name: String,
    val displayName: String,
    val abbreviation: String,
    val season: RestLeagueSeason
)
@Serializable
data class RestLeagueSeason (
    val year: String,
    val displayName: String,
    val type: RestType
)
@Serializable
data class RestType (
    val id: String,
    val type: String,
    val name: String
)

fun RestLeague.toDomain(sport: SiteRoute) =
    League(
        id = id,
        sport = sport.name,
        name = name,
        displayName = displayName,
        abbreviation = abbreviation,
        activeSeasonYear = season.year,
        activeSeasonDisplayName = season.displayName,
        activeSeasonType = season.type.type,
        activeSeasonTypeName = season.type.name,
    )
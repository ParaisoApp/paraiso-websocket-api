package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.League as LeagueDomain

@Serializable
data class League(
    @SerialName(ID) val id: String,
    val sport: String,
    val name: String,
    val displayName: String,
    val abbreviation: String,
    val activeSeasonYear: String,
    val activeSeasonDisplayName: String,
    val activeSeasonType: String,
    val activeSeasonTypeName: String
)
fun LeagueDomain.toEntity() = League(
    id = id,
    sport = sport,
    name = name,
    displayName = displayName,
    abbreviation = abbreviation,
    activeSeasonYear = activeSeasonYear,
    activeSeasonDisplayName = activeSeasonDisplayName,
    activeSeasonType = activeSeasonType,
    activeSeasonTypeName = activeSeasonTypeName
)
fun League.toDomain() = LeagueDomain(
    id = id,
    sport = sport,
    name = name,
    displayName = displayName,
    abbreviation = abbreviation,
    activeSeasonYear = activeSeasonYear,
    activeSeasonDisplayName = activeSeasonDisplayName,
    activeSeasonType = activeSeasonType,
    activeSeasonTypeName = activeSeasonTypeName
)

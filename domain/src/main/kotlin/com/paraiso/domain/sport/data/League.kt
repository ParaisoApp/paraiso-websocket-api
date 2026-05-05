package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class League(
    val id: String,
    val sport: String,
    val name: String,
    val displayName: String,
    val abbreviation: String,
    val activeSeasonYear: String,
    val activeSeasonDisplayName: String,
    val activeSeasonType: String,
    val activeSeasonTypeName: String,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

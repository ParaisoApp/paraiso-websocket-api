package com.paraiso.domain.sport.data

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String?,
    val teamId: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
)

package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    @SerialName(Constants.ID) val id: String,
    val location: String,
    val name: String?,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String?
) { companion object }

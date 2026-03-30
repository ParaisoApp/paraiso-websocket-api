package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Athlete as AthleteDomain
@Serializable
data class Athlete(
    @SerialName(Constants.ID) val id: String,
    val teamAbbr: String?,
    val displayName: String,
    val shortName: String?,
    val jersey: String?,
    val positionName: String?,
    val positionAbbreviation: String?,
    val displayWeight: String?,
    val displayHeight: String?,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String?,
    val ejected: Boolean,
    val stats: List<String>
)

fun AthleteDomain.toEntity() =
    Athlete(
        id = id,
        teamAbbr = teamAbbr,
        displayName = displayName,
        shortName = shortName,
        jersey = jersey,
        positionName = positionName,
        positionAbbreviation = positionAbbreviation,
        displayWeight = displayWeight,
        displayHeight = displayHeight,
        starter = starter,
        didNotPlay = didNotPlay,
        reason = reason,
        ejected = ejected,
        stats = stats
    )

fun Athlete.toDomain() =
    AthleteDomain(
        id = id,
        teamAbbr = teamAbbr,
        displayName = displayName,
        shortName = shortName,
        jersey = jersey,
        positionName = positionName,
        positionAbbreviation = positionAbbreviation,
        displayWeight = displayWeight,
        displayHeight = displayHeight,
        starter = starter,
        didNotPlay = didNotPlay,
        reason = reason,
        ejected = ejected,
        stats = stats
    )

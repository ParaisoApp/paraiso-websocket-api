package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Roster(
    @SerialName(ID) val id: String,
    val athletes: List<Athlete>,
    val coach: Coach?,
    val team: Team
)

@Serializable
data class Athlete(
    @SerialName(ID) val id: String,
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

@Serializable
data class Coach(
    @SerialName(ID) val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
)

@Serializable
data class RosterResponse(
    val id: String,
    val athletes: List<AthleteResponse>,
    val coach: CoachResponse?,
    val team: TeamResponse
)

@Serializable
data class AthleteResponse(
    val id: String,
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

@Serializable
data class CoachResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
)

fun Roster.toResponse() =
    RosterResponse(
        id = id,
        athletes = athletes.map { it.toResponse() },
        coach = coach?.toResponse(),
        team = team.toResponse(),
    )

fun Athlete.toResponse() =
    AthleteResponse(
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
fun Coach.toResponse() =
    CoachResponse(
        id = id,
        firstName = firstName,
        lastName = lastName,
        experience = experience,
    )
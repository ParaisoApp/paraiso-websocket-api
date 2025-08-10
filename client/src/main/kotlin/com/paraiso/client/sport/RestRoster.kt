package com.paraiso.client.sport

import com.paraiso.domain.sport.data.Coach
import com.paraiso.domain.sport.data.createUnknown
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Athlete as AthleteDomain
import com.paraiso.domain.sport.data.Coach as CoachDomain
import com.paraiso.domain.sport.data.Roster as RosterDomain

@Serializable
data class RestRoster(
    val athletes: List<RestAthlete>,
    val coach: List<RestCoach>,
    val team: RestTeam
)

@Serializable
data class RestAthlete(
    val id: String,
    val displayName: String,
    val shortName: String? = null,
    val displayWeight: String? = null,
    val displayHeight: String? = null,
    val jersey: String? = null,
    val position: Position? = null
)

@Serializable
data class RestCoach(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
)

fun RestRoster.toDomain() = RosterDomain(
    athletes = athletes.map { it.toDomain(team.abbreviation) },
    coach = coach.firstOrNull()?.toDomain() ?: Coach.createUnknown(),
    team = team.toDomain()
)

fun RestAthlete.toDomain(teamAbbr: String) = AthleteDomain(
    id = id,
    teamAbbr = teamAbbr,
    displayName = displayName,
    shortName = shortName ?: UNKNOWN,
    displayWeight = displayWeight ?: UNKNOWN,
    displayHeight = displayHeight ?: UNKNOWN,
    jersey = jersey ?: UNKNOWN,
    positionName = position?.name ?: UNKNOWN,
    positionAbbreviation = position?.abbreviation ?: UNKNOWN,
    didNotPlay = false,
    ejected = false,
    reason = "",
    starter = false,
    stats = emptyList()
)

fun RestCoach.toDomain() = CoachDomain(
    id = id,
    firstName = firstName,
    lastName = lastName,
    experience = experience
)

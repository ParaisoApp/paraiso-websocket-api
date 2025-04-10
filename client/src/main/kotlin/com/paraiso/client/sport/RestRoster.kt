package com.paraiso.client.sport

import com.paraiso.domain.sport.sports.createUnknown
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.sports.Athlete as AthleteDomain
import com.paraiso.domain.sport.sports.Coach as CoachDomain
import com.paraiso.domain.sport.sports.Roster as RosterDomain

@Serializable
data class RestRoster(
    val athletes: List<RestAthlete>,
    val coach: List<RestCoach>,
    val team: Team
)

@Serializable
data class RestAthlete(
    val id: String,
    val displayName: String,
    val shortName: String,
    val displayWeight: String? = null,
    val displayHeight: String? = null,
    val jersey: String? = null,
    val position: Position
)

@Serializable
data class RestCoach(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
)

fun RestRoster.toDomain() = RosterDomain(
    athletes = athletes.map { it.toDomain() },
    coach = coach.firstOrNull()?.toDomain() ?: CoachDomain.createUnknown(),
    team = team.toDomain()
)

fun RestAthlete.toDomain() = AthleteDomain(
    id = id,
    displayName = displayName,
    shortName = shortName,
    displayWeight = displayWeight ?: "",
    displayHeight = displayHeight ?: "",
    jersey = jersey ?: "",
    positionName = position.name,
    positionAbbreviation = position.abbreviation,
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

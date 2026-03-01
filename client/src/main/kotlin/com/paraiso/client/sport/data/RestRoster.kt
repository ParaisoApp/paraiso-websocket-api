package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
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
data class RestRosterNested(
    val athletes: List<RestAthleteNested>,
    val coach: List<RestCoach>,
    val team: RestTeam
)

@Serializable
data class RestAthleteNested(
    val position: String,
    val items: List<RestAthlete>
)

@Serializable
data class RestAthlete(
    val id: String,
    val displayName: String,
    val shortName: String? = null,
    val displayWeight: String? = null,
    val displayHeight: String? = null,
    val jersey: String? = null,
    val position: RestPosition? = null
)

@Serializable
data class RestCoach(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int? = 0
)

@Serializable
data class RestPosition(
    val name: String,
    val abbreviation: String
)

fun RestRosterNested.toDomain(sport: SiteRoute) = RosterDomain(
    id = "$sport-${team.id}",
    sport = sport,
    athletes = athletes.flatMap { restAthleteNested -> restAthleteNested.items.map { it.toDomain(team.abbreviation) } },
    coach = coach.firstOrNull()?.toDomain(),
    teamId = team.id
)

fun RestRoster.toDomain(sport: SiteRoute) = RosterDomain(
    id = "$sport-${team.id}",
    sport = sport,
    athletes = athletes.map { it.toDomain(team.abbreviation) },
    coach = coach.firstOrNull()?.toDomain(),
    teamId = team.id
)

fun RestAthlete.toDomain(teamAbbr: String) = AthleteDomain(
    id = id,
    teamAbbr = teamAbbr,
    displayName = displayName,
    shortName = shortName,
    displayWeight = displayWeight,
    displayHeight = displayHeight,
    jersey = jersey,
    positionName = position?.name,
    positionAbbreviation = position?.abbreviation,
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
    experience = experience ?: 0
)

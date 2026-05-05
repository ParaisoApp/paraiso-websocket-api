package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.data.StandingsGroup as StandingsGroupDomain
import com.paraiso.domain.sport.data.StandingsSubGroup as StandingsSubGroupDomain
import com.paraiso.domain.sport.data.Standings as StandingsDomain
import com.paraiso.domain.sport.data.StandingsStat as StandingsStatDomain

@Serializable
data class AllStandings(
    @SerialName(ID) val id: String,
    val standingsGroups: List<StandingsGroup>,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

@Serializable
data class StandingsGroup(
    val confName: String,
    val confAbbr: String,
    val standings: List<Standings>,
    val subGroups: List<StandingsSubGroup>
)

@Serializable
data class StandingsSubGroup(
    val divName: String,
    val divAbbr: String,
    val standings: List<Standings>
)

@Serializable
data class Standings(
    val teamId: String,
    val seed: Int?,
    val stats: List<StandingsStat>
)

@Serializable
data class StandingsStat(
    val shortDisplayName: String?,
    val displayValue: String?,
    val displayName: String?,
    val value: Double?
)

@Serializable
data class StandingsResponse(
    val teamId: String,
    val team: Team?,
    val seed: Int,
    val confName: String,
    val confAbbr: String,
    val divName: String?,
    val divAbbr: String?,
    val stats: Map<String, StandingsStatResponse>
)

@Serializable
data class StandingsStatResponse(
    val shortDisplayName: String?,
    val displayValue: String?,
    val displayName: String?,
    val value: Double?
)

fun AllStandingsDomain.toEntity() =
    AllStandings(
        id = id,
        standingsGroups = standingsGroups.map { it.toEntity() },
        // fields are set in DB layer during save
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun StandingsGroupDomain.toEntity() =
    StandingsGroup(
        confName = confName,
        confAbbr = confAbbr,
        standings = standings.map { it.toEntity() },
        subGroups = subGroups.map { it.toEntity() },
    )

fun StandingsSubGroupDomain.toEntity() =
    StandingsSubGroup(
        divName = divName,
        divAbbr = divAbbr,
        standings = standings.map { it.toEntity() },
    )

fun StandingsDomain.toEntity() =
    Standings(
        teamId = teamId,
        seed = seed,
        stats = stats.map { it.toEntity() },
    )

fun StandingsStatDomain.toEntity() =
    StandingsStat(
        shortDisplayName = shortDisplayName,
        displayValue = displayValue,
        displayName = displayName,
        value = value,
    )

fun AllStandings.toDomain() =
    AllStandingsDomain(
        id = id,
        standingsGroups = standingsGroups.map { it.toDomain() },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun StandingsGroup.toDomain() =
    StandingsGroupDomain(
        confName = confName,
        confAbbr = confAbbr,
        standings = standings.map { it.toDomain() },
        subGroups = subGroups.map { it.toDomain() },
    )

fun StandingsSubGroup.toDomain() =
    StandingsSubGroupDomain(
        divName = divName,
        divAbbr = divAbbr,
        standings = standings.map { it.toDomain() },
    )

fun Standings.toDomain() =
    StandingsDomain(
        teamId = teamId,
        seed = seed,
        stats = stats.map { it.toDomain() },
    )

fun StandingsStat.toDomain() =
    StandingsStatDomain(
        shortDisplayName = shortDisplayName,
        displayValue = displayValue,
        displayName = displayName,
        value = value,
    )

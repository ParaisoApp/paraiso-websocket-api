package com.paraiso.client.sport

import com.paraiso.domain.util.Constants.EMPTY
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Athlete as AthleteDomain
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.data.FullTeam as FullTeamDomain
import com.paraiso.domain.sport.data.StatTypes as StatTypesDomain
import com.paraiso.domain.sport.data.TeamStat as TeamStatDomain

@Serializable
data class RestGameStats(
    val boxscore: BoxScore
)

@Serializable
data class BoxScore(
    val teams: List<TeamWithStats>,
    val players: List<Player>? = null
)

@Serializable
data class TeamWithStats(
    val team: RestTeam,
    val statistics: List<TeamStat>
)

@Serializable
data class TeamStat(
    val displayValue: String,
    val abbreviation: String? = null,
    val label: String
)

@Serializable
data class Player(
    val team: RestTeam,
    val statistics: List<Statistic>
)

@Serializable
data class Statistic(
    val name: String? = null,
    val names: List<String>? = null,
    val descriptions: List<String>,
    val athletes: List<AthleteBase>
)

@Serializable
data class AthleteBase(
    val athlete: RestAthlete,
    val starter: Boolean? = null,
    val didNotPlay: Boolean? = null,
    val reason: String? = null,
    val ejected: Boolean? = null,
    val stats: List<String>
)

@Serializable
data class Position(
    val name: String,
    val abbreviation: String
)

fun RestGameStats.toDomain() = BoxScoreDomain(
    teams = boxscore.teams.map { it.toDomain(boxscore.players) }
)

fun TeamWithStats.toDomain(players: List<Player>?): FullTeamDomain {
    val stats = players?.first { it.team.id == team.id }?.statistics?.first()
    return FullTeamDomain(
        teamId = team.id,
        teamStats = statistics.map { it.toDomain() },
        statTypes = stats?.toDomain() ?: StatTypesDomain(name= UNKNOWN, names = emptyList(), descriptions = emptyList()),
        athletes = stats?.athletes?.map { it.toDomain() }
    )
}

fun TeamStat.toDomain() = TeamStatDomain(
    displayValue = displayValue,
    abbreviation = abbreviation ?: UNKNOWN,
    label = label
)

fun Statistic.toDomain() = StatTypesDomain(
    name = name ?: UNKNOWN,
    names = names ?: emptyList(),
    descriptions = descriptions
)

fun AthleteBase.toDomain() = AthleteDomain(
    id = athlete.id,
    teamAbbr = EMPTY,
    displayName = athlete.displayName,
    shortName = athlete.shortName ?: UNKNOWN,
    jersey = athlete.jersey ?: UNKNOWN,
    positionName = athlete.position?.name ?: UNKNOWN,
    positionAbbreviation = athlete.position?.abbreviation ?: UNKNOWN,
    starter = starter ?: false,
    didNotPlay = didNotPlay ?: false,
    reason = reason ?: EMPTY,
    ejected = ejected ?: false,
    stats = stats,
    displayHeight = EMPTY,
    displayWeight = EMPTY
)

package com.paraiso.client.sport

import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.sports.Athlete as AthleteDomain
import com.paraiso.domain.sport.sports.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.sports.FullTeam as FullTeamDomain
import com.paraiso.domain.sport.sports.StatTypes as StatTypesDomain
import com.paraiso.domain.sport.sports.TeamStat as TeamStatDomain

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
    val team: Team,
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
    val team: Team,
    val statistics: List<Statistic>
)

@Serializable
data class Statistic(
    val names: List<String>,
    val descriptions: List<String>,
    val athletes: List<AthleteBase>
)

@Serializable
data class AthleteBase(
    val athlete: RestAthlete,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String? = null,
    val ejected: Boolean,
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
        statTypes = stats?.toDomain() ?: StatTypesDomain(names = emptyList(), descriptions = emptyList()),
        athletes = stats?.athletes?.map { it.toDomain() }
    )
}

fun TeamStat.toDomain() = TeamStatDomain(
    displayValue = displayValue,
    abbreviation = abbreviation ?: UNKNOWN,
    label = label
)

fun Statistic.toDomain() = StatTypesDomain(
    names = names,
    descriptions = descriptions
)

fun AthleteBase.toDomain() = AthleteDomain(
    id = athlete.id,
    displayName = athlete.displayName,
    shortName = athlete.shortName,
    jersey = athlete.jersey ?: UNKNOWN,
    positionName = athlete.position.name,
    positionAbbreviation = athlete.position.abbreviation,
    starter = starter,
    didNotPlay = didNotPlay,
    reason = reason ?: "",
    ejected = ejected,
    stats = stats,
    displayHeight = "",
    displayWeight = ""
)

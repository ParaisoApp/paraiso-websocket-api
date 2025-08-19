package com.paraiso.client.sport

import com.paraiso.domain.routes.SiteRoute
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

fun RestGameStats.toDomain(sport: SiteRoute, competitionId: String) = BoxScoreDomain(
    id = competitionId,
    teams = boxscore.teams.map { it.toDomain(boxscore.players, sport) }
)

fun TeamWithStats.toDomain(players: List<Player>?, sport: SiteRoute): FullTeamDomain {
    val stats = players?.first { it.team.id == team.id }?.statistics?.first()
    return FullTeamDomain(
        teamId = team.id,
        teamStats = statistics.map { it.toDomain() },
        statTypes = stats?.toDomain(),
        athletes = stats?.athletes?.map { it.toDomain() }
    )
}

fun TeamStat.toDomain() = TeamStatDomain(
    displayValue = displayValue,
    abbreviation = abbreviation,
    label = label
)

fun Statistic.toDomain() = StatTypesDomain(
    name = name,
    names = names ?: emptyList(),
    descriptions = descriptions
)

fun AthleteBase.toDomain() = AthleteDomain(
    id = athlete.id,
    teamAbbr = null,
    displayName = athlete.displayName,
    shortName = athlete.shortName,
    jersey = athlete.jersey,
    positionName = athlete.position?.name,
    positionAbbreviation = athlete.position?.abbreviation,
    starter = starter ?: false,
    didNotPlay = didNotPlay ?: false,
    reason = reason,
    ejected = ejected ?: false,
    stats = stats,
    displayHeight = null,
    displayWeight = null
)

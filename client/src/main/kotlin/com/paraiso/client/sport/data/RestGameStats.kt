package com.paraiso.client.sport.data

import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Athlete as AthleteDomain
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.data.FullTeam as FullTeamDomain
import com.paraiso.domain.sport.data.StatTypes as StatTypesDomain
import com.paraiso.domain.sport.data.TeamStat as TeamStatDomain

@Serializable
data class RestGameStats(
    val boxscore: RestBoxScore
)

@Serializable
data class RestBoxScore(
    val teams: List<RestTeamWithStats>,
    val players: List<RestPlayer>? = null
)

@Serializable
data class RestTeamWithStats(
    val team: RestTeam,
    val statistics: List<RestTeamStat>
)

@Serializable
data class RestTeamStat(
    val displayValue: String,
    val abbreviation: String? = null,
    val label: String
)

@Serializable
data class RestPlayer(
    val team: RestTeam,
    val statistics: List<RestStatistic>
)

@Serializable
data class RestStatistic(
    val name: String? = null,
    val descriptions: List<String>,
    val labels: List<String>,
    val athletes: List<RestAthleteBase>
)

@Serializable
data class RestAthleteBase(
    val athlete: RestAthlete,
    val starter: Boolean? = null,
    val didNotPlay: Boolean? = null,
    val reason: String? = null,
    val ejected: Boolean? = null,
    val stats: List<String>
)

fun RestGameStats.toDomain(competitionId: String) = BoxScoreDomain(
    id = competitionId,
    teams = boxscore.teams.map { it.toDomain(boxscore.players) }
)

fun RestTeamWithStats.toDomain(players: List<RestPlayer>?): FullTeamDomain {
    val stats = players?.first { it.team.id == team.id }?.statistics
    return FullTeamDomain(
        teamId = team.id,
        teamStats = statistics.map { it.toDomain() },
        statTypes = stats?.map { it.toDomain() } ?: emptyList()
    )
}

fun RestTeamStat.toDomain() = TeamStatDomain(
    displayValue = displayValue,
    abbreviation = abbreviation,
    label = label
)

fun RestStatistic.toDomain() = StatTypesDomain(
    name = name,
    labels = labels,
    descriptions = descriptions,
    athletes = athletes.map { it.toDomain() }
)

fun RestAthleteBase.toDomain() = AthleteDomain(
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

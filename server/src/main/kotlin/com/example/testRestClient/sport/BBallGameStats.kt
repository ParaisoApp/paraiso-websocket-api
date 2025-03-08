package com.example.testRestClient.sport

import kotlinx.serialization.Serializable
import com.example.messageTypes.BoxScore as BoxScoreDomain
import com.example.messageTypes.FullTeam as FullTeamDomain
import com.example.messageTypes.TeamStat as TeamStatDomain
import com.example.messageTypes.StatTypes as StatTypesDomain
import com.example.messageTypes.Athlete as AthleteDomain

@Serializable
data class BBallGameStats (
    val boxscore: BoxScore
)

@Serializable
data class BoxScore (
    val teams: List<TeamWithStats>,
    val players: List<Player>? = emptyList()
)

@Serializable
data class TeamWithStats(
    val team: Team,
    val statistics: List<TeamStat>
)

@Serializable
data class TeamStat(
    val displayValue: String,
    val abbreviation: String? = "",
    val label: String
)

@Serializable
data class Player (
    val team: Team,
    val statistics: List<Statistic>
)

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String,
)

@Serializable
data class Statistic (
    val names: List<String>,
    val descriptions: List<String>,
    val athletes: List<AthleteBase>
)

@Serializable
data class AthleteBase (
    val athlete: Athlete,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String,
    val ejected: Boolean,
    val stats: List<String>,
)

@Serializable
data class Athlete (
    val id: String,
    val displayName: String,
    val shortName: String,
    val jersey: String? = "",
    val position: Position
)

@Serializable
data class Position (
    val name: String,
    val abbreviation: String,
)

fun BBallGameStats.toDomain() = BoxScoreDomain(
    teams = boxscore.teams.map { it.toDomain(boxscore.players) }
)

fun TeamWithStats.toDomain(players: List<Player>?): FullTeamDomain {
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
    names = names,
    descriptions = descriptions
)

fun AthleteBase.toDomain() = AthleteDomain(
    id = athlete.id,
    displayName = athlete.displayName,
    shortName = athlete.shortName,
    jersey = athlete.jersey,
    positionName = athlete.position.name,
    positionAbbreviation = athlete.position.abbreviation,
    starter = starter,
    didNotPlay = didNotPlay,
    reason = reason,
    ejected = ejected,
    stats = stats
)
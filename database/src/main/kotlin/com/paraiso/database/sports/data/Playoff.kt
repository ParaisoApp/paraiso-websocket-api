package com.paraiso.database.sports.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Playoff as PlayoffDomain
import com.paraiso.domain.sport.data.PlayoffRound as PlayoffRoundDomain
import com.paraiso.domain.sport.data.PlayoffMatchUp as PlayoffMatchUpDomain
import com.paraiso.domain.sport.data.PlayoffTeam as PlayoffTeamDomain

@Serializable
data class Playoff(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val year: Int,
    val rounds: List<PlayoffRound>
)

@Serializable
data class PlayoffRound(
    val round: Int,
    val matchUps: List<PlayoffMatchUp>
)

@Serializable
data class PlayoffMatchUp(
    val id: String,
    val compIds: Set<String>,
    val teams: List<PlayoffTeam>
)

@Serializable
data class PlayoffTeam(
    val id: String,
    val score: Int,
    val winner: Boolean?
)

fun PlayoffDomain.toEntity() =
    Playoff(
        id = id,
        sport = sport,
        year = year,
        rounds = rounds.values.map { it.toEntity() }
    )

fun PlayoffRoundDomain.toEntity() =
    PlayoffRound(
        round = round,
        matchUps = matchUps.values.map { it.toEntity() }
    )

fun PlayoffMatchUpDomain.toEntity() =
    PlayoffMatchUp(
        id = id,
        compIds = compIds,
        teams = teams.values.map { it.toEntity() }
    )
fun PlayoffTeamDomain.toEntity() =
    PlayoffTeam(
        id = id,
        score = score,
        winner = winner
    )

fun Playoff.toDomain() =
    PlayoffDomain(
        id = id,
        sport = sport,
        year = year,
        rounds = rounds.associate { it.round to it.toDomain() }
    )

fun PlayoffRound.toDomain() =
    PlayoffRoundDomain(
        round = round,
        winners = matchUps.flatMap { it.teams }.filter { it.winner == true }.map { it.id },
        matchUps = matchUps.associate { it.id to it.toDomain() }
    )

fun PlayoffMatchUp.toDomain() =
    PlayoffMatchUpDomain(
        id = id,
        compIds = compIds.toMutableSet(),
        teams = teams.associate { it.id to it.toDomain() }
    )
fun PlayoffTeam.toDomain() =
    PlayoffTeamDomain(
        id = id,
        score = score,
        winner = winner
    )

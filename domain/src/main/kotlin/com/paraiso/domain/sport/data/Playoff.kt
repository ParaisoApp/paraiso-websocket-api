package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val teams: List<PlayoffTeam>
)
@Serializable
data class PlayoffTeam(
    val id: String,
    val score: Int,
    val winner: Boolean?
)
@Serializable
data class PlayoffResponse(
    val id: String,
    val sport: SiteRoute,
    val year: Int,
    val rounds: Map<Int, PlayoffRoundResponse>
)
@Serializable
data class PlayoffRoundResponse(
    val round: Int,
    val matchUps: Map<String, PlayoffMatchUpResponse>
)
@Serializable
data class PlayoffMatchUpResponse(
    val id: String,
    val teams: Map<String, PlayoffTeamResponse>
)
@Serializable
data class PlayoffTeamResponse(
    val id: String,
    val score: Int,
    val winner: Boolean?
)

fun Playoff.toResponse() =
    PlayoffResponse(
        id = id,
        sport = sport,
        year = year,
        rounds = rounds.associate { it.round to it.toResponse() }
    )

fun PlayoffRound.toResponse() =
    PlayoffRoundResponse(
        round = round,
        matchUps.associate { it.teams.map { team -> team.id }.sorted().joinToString { "-" } to it.toResponse() }
    )

fun PlayoffMatchUp.toResponse() =
    PlayoffMatchUpResponse(
        id = id,
        teams.associate { it.id to it.toResponse() }
    )
fun PlayoffTeam.toResponse() =
    PlayoffTeamResponse(
        id = id,
        score = score,
        winner = winner
    )

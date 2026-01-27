package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Playoff(
    @SerialName(ID) val id: String,
    val sport: SiteRoute,
    val year: Int,
    val rounds: Map<String, PlayoffRound>
)
@Serializable
data class PlayoffRound(
    val round: Int,
    val matchups: List<PlayoffMatchup>
)
@Serializable
data class PlayoffMatchup(
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
    val matchups: Map<String, PlayoffMatchupResponse>
)
@Serializable
data class PlayoffMatchupResponse(
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
        rounds = rounds.map { (it.key.toIntOrNull() ?: 0) to it.value.toResponse() }.toMap()
    )

fun PlayoffRound.toResponse() =
    PlayoffRoundResponse(
        matchups.associate { "${it.teams[0].id}-${it.teams[1].id}" to it.toResponse() }
    )

fun PlayoffMatchup.toResponse() =
    PlayoffMatchupResponse(
        teams.associate { it.id to it.toResponse() }
    )
fun PlayoffTeam.toResponse() =
    PlayoffTeamResponse(
        id = id,
        score = score,
        winner = winner
    )

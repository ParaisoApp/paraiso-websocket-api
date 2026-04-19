package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Playoff(
    val id: String,
    val sport: SiteRoute,
    val year: Int,
    val rounds: Map<Int, PlayoffRound>
)

@Serializable
data class PlayoffRound(
    val round: Int,
    val winners: List<String>,
    val matchUps: Map<String, PlayoffMatchUp>
)

@Serializable
data class PlayoffMatchUp(
    val id: String,
    val compIds: MutableSet<String>,
    val teams: Map<String, PlayoffTeam>
)

@Serializable
data class PlayoffTeam(
    val id: String,
    val score: Int,
    val winner: Boolean?
)

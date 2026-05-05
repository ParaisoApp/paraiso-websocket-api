package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String,
    val sport: SiteRoute,
    val season: Season,
    val teamId: String,
    val events: List<Competition>,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

@Serializable
data class Season(
    val year: Int,
    val type: Int,
    val name: String?,
    val displayName: String?
)

fun ScoreboardBasic.toFullData(competitions: List<Competition>) = Scoreboard(
    id = id,
    sport = sport,
    season = season,
    week = week,
    day = day,
    competitions = competitions,
    createdOn = null,
    updatedOn = null
)

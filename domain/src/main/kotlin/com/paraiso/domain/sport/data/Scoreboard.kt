package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Scoreboard(
    val id: String,
    val sport: String?,
    val season: Season?,
    val week: Int?,
    val day: Instant?,
    val competitions: List<Competition>,
    val createdOn: Instant?,
    val updatedOn: Instant?
) { companion object }

@Serializable
data class TeamYearStats(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String?
)

@Serializable
data class Record(
    val name: String?,
    val summary: String?
)

@Serializable
data class ScoreboardBasic(
    val id: String,
    val sport: String?,
    val season: Season?,
    val week: Int?,
    val day: Instant?,
    val competitions: List<String>
)

fun Scoreboard.toBasic() =
    ScoreboardBasic(
        id = id,
        sport = sport,
        season = season,
        week = week,
        day = day,
        competitions = competitions.map { it.id }
    )

fun Scoreboard.Companion.init() =
    Scoreboard(
        id = "UNKNOWN",
        sport = null,
        season = Season(
            year = 1900,
            type = 1,
            name = null,
            displayName = null
        ),
        week = null,
        day = null,
        competitions = emptyList(),
        createdOn = null,
        updatedOn = null
    )

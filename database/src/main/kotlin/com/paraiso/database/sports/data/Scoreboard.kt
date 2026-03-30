package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.data.TeamYearStats as TeamYearStatsDomain
import com.paraiso.domain.sport.data.Record as RecordDomain

@Serializable
data class Scoreboard(
    @SerialName(ID) val id: String,
    val sport: String?,
    val season: Season?,
    val week: Int?,
    @Serializable(with = InstantBsonSerializer::class)
    val day: Instant?,
    val competitions: List<String>
)

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

fun ScoreboardDomain.toEntity() =
    Scoreboard(
        id = id,
        sport = sport,
        season = season?.toEntity(),
        week = week,
        day = day,
        competitions = competitions.map { it.id }
    )

fun RecordDomain.toEntity() =
    Record(
        name = name,
        summary = summary
    )

fun TeamYearStatsDomain.toEntity() =
    TeamYearStats(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue
    )

fun Scoreboard.toDomain() =
    ScoreboardDomain(
        id = id,
        sport = sport,
        season = season?.toDomain(),
        week = week,
        day = day,
        competitions = emptyList()
    )

fun Record.toDomain() =
    RecordDomain(
        name = name,
        summary = summary
    )

fun TeamYearStats.toDomain() =
    TeamYearStatsDomain(
        name = name,
        abbreviation = abbreviation,
        displayValue = displayValue,
        rankDisplayValue = rankDisplayValue
    )

package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.util.convertStringToInstant
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Record as RecordDomain
import com.paraiso.domain.sport.data.Status as StatusDomain
import com.paraiso.domain.sport.data.TeamYearStats as TeamYearStatsDomain

@Serializable
data class RestScoreboard(
    val season: RestSeason,
    val week: RestWeek? = null,
    val day: RestDay? = null,
    val events: List<RestEvent>
)

@Serializable
data class RestWeek(
    val number: Int
)

@Serializable
data class RestDay(
    val date: String
)

@Serializable
data class TeamYearStats(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String? = null
)

@Serializable
data class LineScore(
    val value: Double
)

@Serializable
data class Record(
    val name: String,
    val summary: String
)

@Serializable
data class Status(
    val displayClock: String,
    val period: Int,
    val type: Type
)

@Serializable
data class Type(
    val name: String,
    val state: String,
    val completed: Boolean
)

fun RestScoreboard.toDomain(sport: SiteRoute): Scoreboard {
    var id = "$sport-${season.type}-${season.year}"
    if (week != null) id += "-${week.number}"
    if (day != null) id += "-${day.date}"
    return Scoreboard(
        id = id,
        sport = sport.name,
        season = season.toDomain(),
        week = week?.number,
        day = convertStringToInstant(day?.date),
        competitions = this.events.map { it.competitions.first().toDomain(it.name, it.shortName, week?.number, season, sport) }
    )
}

fun TeamYearStats.toDomain() = TeamYearStatsDomain(
    name = name,
    abbreviation = abbreviation,
    displayValue = displayValue,
    rankDisplayValue = rankDisplayValue
)

fun Record.toDomain() = RecordDomain(
    name = name,
    summary = summary
)

fun Status.toDomain(): StatusDomain {
    return StatusDomain(
        clock = displayClock,
        period = period,
        name = type.name,
        state = type.state,
        completed = type.completed,
        completedTime = null
    )
}

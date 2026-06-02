package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.util.convertStringToInstant
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Situation as SituationDomain

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
        competitions = this.events.map { it.competitions.first().toDomain(it.name, it.shortName, season.toDomain(), week?.number, sport) },
        createdOn = null,
        updatedOn = null
    )
}

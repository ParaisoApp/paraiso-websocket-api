package com.paraiso.client.sport

import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.sports.Competition as CompetitionDomain
import com.paraiso.domain.sport.sports.Record as RecordDomain
import com.paraiso.domain.sport.sports.Status as StatusDomain
import com.paraiso.domain.sport.sports.TeamGameStats as TeamGameStatsDomain
import com.paraiso.domain.sport.sports.TeamYearStats as TeamYearStatsDomain
import com.paraiso.domain.sport.sports.Venue as VenueDomain

@Serializable
data class RestScoreboard(
    val events: List<Event>
)

@Serializable
data class Event(
    val name: String,
    val shortName: String,
    val date: String,
    val competitions: List<Competition>
)

@Serializable
data class Competition(
    val id: String,
    val venue: Venue,
    val competitors: List<Competitor>,
    val status: Status
)

@Serializable
data class Competitor(
    val homeAway: String,
    val team: Team,
    val winner: Boolean? = null,
    val score: String,
    val statistics: List<TeamYearStats>,
    val linescores: List<LineScore>? = null,
    val records: List<Record>
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
data class Venue(
    val fullName: String,
    val address: Address
)

@Serializable
data class Address(
    val city: String,
    val state: String
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

fun RestScoreboard.toDomain() = Scoreboard(
    competitions = this.events.map { it.competitions.first().toDomain(it.name, it.shortName, it.date) }
)

fun Competition.toDomain(name: String, shortName: String, date: String) = CompetitionDomain(
    id = id,
    name = name,
    shortName = shortName,
    date = date,
    teams = competitors.map { it.toTeamDomain() },
    venue = venue.toDomain(),
    status = status.toDomain()
)

fun Competitor.toTeamDomain() = TeamGameStatsDomain(
    team = team.toDomain(),
    homeAway = homeAway,
    records = records.map { it.toDomain() },
    winner = winner ?: false,
    teamYearStats = statistics.map { it.toDomain() },
    lineScores = linescores?.map { it.value } ?: emptyList(),
    score = score
)

fun TeamYearStats.toDomain() = TeamYearStatsDomain(
    name = name,
    abbreviation = abbreviation,
    displayValue = displayValue,
    rankDisplayValue = rankDisplayValue ?: UNKNOWN
)

fun Record.toDomain() = RecordDomain(
    name = name,
    summary = summary
)

fun Venue.toDomain() = VenueDomain(
    fullName = fullName,
    city = address.city,
    state = address.state
)

fun Status.toDomain(): StatusDomain {
    return StatusDomain(
        clock = displayClock,
        period = period,
        name = type.name,
        state = type.state,
        completed = type.completed
    )
}

package com.example.testRestClient.sport

import com.example.messageTypes.Scoreboard
import kotlinx.serialization.Serializable
import com.example.messageTypes.Competition as CompetitionDomain
import com.example.messageTypes.Record as RecordDomain
import com.example.messageTypes.Status as StatusDomain
import com.example.messageTypes.Team as TeamDomain
import com.example.messageTypes.Venue as VenueDomain

@Serializable
data class BBallScoreboard(
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
    val winner: Boolean? = false,
    val score: String,
    val linescores: List<LineScore>? = null,
    val records: List<Record>
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

fun BBallScoreboard.toDomain() = Scoreboard(
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

fun Competitor.toTeamDomain() = TeamDomain(
    id = team.id,
    location = team.location,
    name = team.name,
    abbreviation = team.abbreviation,
    displayName = team.displayName,
    shortDisplayName = team.shortDisplayName,
    homeAway = homeAway,
    records = records.map { it.toDomain() },
    winner = winner,
    lineScores = linescores?.map { it.value },
    score = score
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

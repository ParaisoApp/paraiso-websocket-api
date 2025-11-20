package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Status
import com.paraiso.domain.sport.data.TeamGameStats
import com.paraiso.domain.sport.data.TeamYearStats
import com.paraiso.domain.util.convertStringZToInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import com.paraiso.domain.sport.data.Competition as CompetitionDomain
import com.paraiso.domain.sport.data.Record as RecordDomain
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.data.Season as SeasonDomain
import com.paraiso.domain.sport.data.Venue as VenueDomain

@Serializable
data class RestSchedule(
    val season: RestSeason,
    val team: RestTeam,
    val events: List<RestEvent>
)

@Serializable
data class RestEvent(
    val id: String,
    val name: String,
    val shortName: String,
    val week: RestWeek? = null,
    val competitions: List<RestCompetition>
)

@Serializable
data class RestCompetition(
    val id: String,
    val venue: RestVenue,
    val date: String,
    val competitors: List<RestCompetitor>,
    val situation: RestSituation? = null,
    val status: RestStatus
)

@Serializable
data class RestStatus(
    val displayClock: String,
    val period: Int,
    val type: RestType
)

@Serializable
data class RestType(
    val name: String,
    val state: String,
    val completed: Boolean
)

@Serializable
data class RestVenue(
    val fullName: String,
    val address: RestAddress
)

@Serializable
data class RestAddress(
    val city: String,
    val state: String? = null
)

@Serializable
data class RestCompetitor(
    val homeAway: String,
    val team: RestTeam,
    val winner: Boolean? = null,
    @Serializable(with = ScoreSerializer::class)
    val score: RestScore? = null,
    val statistics: List<RestTeamYearStats>? = null,
    val linescores: List<RestLineScore>? = null,
    val records: List<RestRecord>? = null,
    @Serializable(with = RecordSerializer::class)
    val record: List<RestRecordYTD>? = null
)

@Serializable
data class RestTeamYearStats(
    val name: String,
    val abbreviation: String,
    val displayValue: String,
    val rankDisplayValue: String? = null
)

@Serializable
data class RestRecord(
    val name: String,
    val summary: String
)

@Serializable
data class RestLineScore(
    val value: Double
)

@Serializable
data class RestRecordYTD(
    val description: String?,
    val displayValue: String?
)

@Serializable
data class RestScore(
    val value: String?,
    val displayValue: String?
)

@Serializable
data class RestSeason(
    val year: Int,
    val type: Int,
    val name: String? = null,
    val displayName: String? = null
)

fun RestSchedule.toDomain(sport: SiteRoute) = ScheduleDomain(
    id = "$sport-${team.id}-${season.year}-${season.type}",
    sport = sport,
    teamId = team.id,
    season = season.toDomain(),
    events = events.map { it.competitions.first().toDomain(it.name, it.shortName, it.week?.number, season, sport) }
)

fun RestSeason.toDomain() = SeasonDomain(
    year = year,
    type = type,
    name = name,
    displayName = displayName
)

fun RestCompetition.toDomain(
    name: String,
    shortName: String,
    week: Int?,
    season: RestSeason,
    sport: SiteRoute
) = CompetitionDomain(
    id = id,
    sport = sport,
    name = name,
    shortName = shortName,
    date = convertStringZToInstant(date),
    week = week,
    season = season.toDomain(),
    teams = competitors.map { it.toTeamDomain() },
    venue = venue.toDomain(),
    situation = situation?.toDomain(),
    status = status.toDomain()
)

fun RestCompetitor.toTeamDomain() = TeamGameStats(
    teamId = team.id,
    homeAway = homeAway,
    records = records?.map { it.toDomain() } ?: record?.map { it.toDomain() } ?: emptyList(),
    winner = winner ?: false,
    teamYearStats = statistics?.map { it.toDomain() } ?: emptyList(),
    lineScores = linescores?.map { it.value } ?: emptyList(),
    score = score?.displayValue
)

fun RestTeamYearStats.toDomain() = TeamYearStats(
    name = name,
    abbreviation = abbreviation,
    displayValue = displayValue,
    rankDisplayValue = rankDisplayValue
)

fun RestRecord.toDomain() = RecordDomain(
    name = name,
    summary = summary
)

fun RestRecordYTD.toDomain() = RecordDomain(
    name = description,
    summary = displayValue
)

fun RestVenue.toDomain() = VenueDomain(
    fullName = fullName,
    city = address.city,
    state = address.state
)

fun RestStatus.toDomain() = Status(
    clock = displayClock,
    period = period,
    name = type.name,
    state = type.state,
    completed = type.completed,
    completedTime = null
)

object ScoreSerializer : KSerializer<RestScore> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ScoreWrapper")

    override fun deserialize(decoder: Decoder): RestScore {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> RestScore(value = element.content, displayValue = element.content)
            is JsonObject -> {
                val value = element["value"]?.jsonPrimitive?.content
                val displayValue = element["displayValue"]?.jsonPrimitive?.content
                RestScore(value = value, displayValue = displayValue)
            }
            else -> throw SerializationException("Unknown type for score")
        }
    }

    override fun serialize(encoder: Encoder, value: RestScore) {
        // no need to serialize
    }
}

object RecordSerializer : KSerializer<List<RestRecordYTD>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RecordWrapper")

    override fun deserialize(decoder: Decoder): List<RestRecordYTD> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonDecoder")

        return when (val jsonElement = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> listOf(
                RestRecordYTD(
                    description = jsonElement.content,
                    displayValue = jsonElement.content
                )
            )
            is JsonArray -> {
                jsonElement.mapNotNull { element ->
                    if (element is JsonObject) {
                        RestRecordYTD(
                            element["description"]?.jsonPrimitive?.content,
                            element["displayValue"]?.jsonPrimitive?.content
                        )
                    } else {
                        null
                    }
                }
            }

            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<RestRecordYTD>) {
        // no need to serialize
    }
}

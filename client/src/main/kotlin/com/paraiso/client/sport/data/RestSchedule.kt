package com.paraiso.client.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.TeamGameStats
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
    val competitions: List<RestCompetition>
)

@Serializable
data class RestCompetition(
    val id: String,
    val venue: Venue,
    val date: String,
    val competitors: List<RestCompetitor>,
    val status: Status
)

@Serializable
data class Venue(
    val fullName: String,
    val address: Address
)

@Serializable
data class Address(
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
    val statistics: List<TeamYearStats>? = null,
    val linescores: List<LineScore>? = null,
    val records: List<Record>? = null,
    @Serializable(with = RecordSerializer::class)
    val record: List<RestRecordYTD>? = null
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
    events = events.map { it.competitions.first().toDomain(it.name, it.shortName) }
)

fun RestSeason.toDomain() = SeasonDomain(
    year = year,
    type = type,
    name = name,
    displayName = displayName
)

fun RestCompetition.toDomain(name: String, shortName: String) = CompetitionDomain(
    id = id,
    name = name,
    shortName = shortName,
    date = date,
    teams = competitors.map { it.toTeamDomain() },
    venue = venue.toDomain(),
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

fun RestRecordYTD.toDomain() = RecordDomain(
    name = description,
    summary = displayValue
)

fun Venue.toDomain() = VenueDomain(
    fullName = fullName,
    city = address.city,
    state = address.state
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

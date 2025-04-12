package com.paraiso.client.sport

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class RestSchedule(
    val season: RestSeason,
    val team: RestTeam,
    val events: List<RestEvent>
)

@Serializable
data class RestEvent(
    val name: String,
    val shortName: String,
    val date: String,
    val competitions: List<RestCompetition>
)

@Serializable
data class RestCompetition(
    val id: String,
    val venue: Venue,
    val competitors: List<RestCompetitor>,
    val status: Status
)

@Serializable
data class RestCompetitor(
    val homeAway: String,
    val team: RestTeam,
    val winner: Boolean? = null,
    val score: ScoreWrapper? = null,
    val statistics: List<TeamYearStats>? = null,
    val linescores: List<LineScore>? = null,
    val records: List<Record>? = null
)

@Serializable(with = ScoreWrapperSerializer::class)
sealed class ScoreWrapper {
    @Serializable
    data class ScoreString(val value: String) : ScoreWrapper()
    @Serializable
    data class ScoreObject(val home: Int, val away: Int) : ScoreWrapper()
}

@Serializable
data class RestScore(
    val value: Double,
    val displayValue: String
)

@Serializable
data class RestSeason(
    val year: Int,
    val type: Int,
    val name: String,
    val displayName: String
)

object ScoreWrapperSerializer : KSerializer<ScoreWrapper> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ScoreWrapper")

    override fun deserialize(decoder: Decoder): ScoreWrapper {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> ScoreWrapper.ScoreString(element.content)
            is JsonObject -> {
                val home = element["home"]?.jsonPrimitive?.intOrNull ?: 0
                val away = element["away"]?.jsonPrimitive?.intOrNull ?: 0
                ScoreWrapper.ScoreObject(home, away)
            }
            else -> throw SerializationException("Unknown type for score")
        }
    }

    override fun serialize(encoder: Encoder, value: ScoreWrapper) {
        val jsonEncoder = encoder as JsonEncoder

        when (value) {
            is ScoreWrapper.ScoreString -> jsonEncoder.encodeString(value.value)
            is ScoreWrapper.ScoreObject -> {
                val json = buildJsonObject {
                    put("home", JsonPrimitive(value.home))
                    put("away", JsonPrimitive(value.away))
                }
                jsonEncoder.encodeJsonElement(json)
            }
        }
    }
}

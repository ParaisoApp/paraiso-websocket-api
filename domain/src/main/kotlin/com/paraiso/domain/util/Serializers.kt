package com.paraiso.domain.util

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.BsonDateTime
import org.bson.BsonString
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder

object RecordSerializer : KSerializer<Set<String>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("UserReceiveIdsSerializer")

    override fun serialize(encoder: Encoder, value: Set<String>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("This serializer only works with JSON")
        val jsonObject = buildJsonObject {
            value.forEach { key ->
                put(key, JsonPrimitive(true))
            }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Set<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer only works with JSON")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        return jsonObject.filterValues { it.jsonPrimitive.boolean }.keys
    }
}

object InstantBsonSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantBsonSerializer", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Instant) {
        if (encoder is BsonEncoder) {
            encoder.encodeBsonValue(BsonDateTime(value.toEpochMilliseconds()))
        } else {
            encoder.encodeString(value.toString()) // fallback for JSON
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Instant {
        return if (decoder is BsonDecoder) {
            when (val bsonValue = decoder.decodeBsonValue()) {
                is BsonDateTime -> Instant.fromEpochMilliseconds(bsonValue.value)
                is BsonString -> Instant.parse(bsonValue.value) // fallback for old string data
                else -> throw IllegalArgumentException("Unsupported BsonValue type: $bsonValue")
            }
        } else {
            Instant.parse(decoder.decodeString())
        }
    }
}

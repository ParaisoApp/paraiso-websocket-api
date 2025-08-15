package com.paraiso.domain.util

import kotlinx.serialization.KSerializer
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

package com.paraiso.server.util

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.users.Country
import com.paraiso.domain.users.UserResponse
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.nio.charset.Charset
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.messageTypes.Message as MessageDomain
import com.paraiso.domain.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.users.UserResponse as UserResponseDomain

val safeList: Safelist = Safelist()
    .addTags(
        "p", "br", "strong", "em", "u", "s", "code", "pre", "blockquote",
        "ul", "ol", "li", "a", "h1", "h2", "h3", "h4", "h5", "h6"
    )
    .addAttributes("a", "href", "target", "rel")
    .addProtocols("a", "href", "http", "https", "mailto")
    .preserveRelativeLinks(true)

fun determineMessageType(frame: Frame): MessageType? {
    return try {
        val rawText = when (frame) {
            is Frame.Text -> frame.readText()
            else -> return null // Not a text frame
        }

        val jsonElement = Json.parseToJsonElement(rawText)
        val typeMapping = jsonElement.jsonObject["typeMapping"]?.jsonObject

        typeMapping?.keys?.firstOrNull()?.let { key ->
            MessageType.valueOf(key)
        }
    } catch (e: Exception) {
        println("Error determining message type: $e")
        null
    }
}

internal suspend inline fun <reified T> WebsocketContentConverter.cleanAndType(
    frame: Frame,
    skipFields: Set<String> = emptySet()
): T? =
    try {
        val cleanFrame = if (frame is Frame.Text) {
            val rawText = frame.readText()
            val cleanedJson = cleanJsonStrings(rawText, skipFields, safeList)
            Frame.Text(cleanedJson)
        } else {
            frame
        }

        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            cleanFrame
        ) as? T
    } catch (e: Exception) {
        println(e)
        null
    }

// Helper: clean JSON selectively
private fun cleanJsonStrings(
    json: String,
    skipFields: Set<String>,
    safeList: Safelist
): String {
    val parser = Json { ignoreUnknownKeys = true }
    val root = parser.parseToJsonElement(json)
    val cleaned = cleanElement(root, skipFields, safeList, parentField = null)
    return Json.encodeToString(JsonElement.serializer(), cleaned)
}

private fun cleanElement(
    element: JsonElement,
    skipFields: Set<String>,
    safeList: Safelist,
    parentField: String?
): JsonElement {
    return when (element) {
        is JsonObject -> JsonObject(element.mapValues { (key, value) ->
            cleanElement(value, skipFields, safeList, key)
        })
        is JsonArray -> JsonArray(element.map { item ->
            cleanElement(item, skipFields, safeList, parentField)
        })
        is JsonPrimitive -> {
            if (element.isString && parentField != null && !skipFields.contains(parentField)) {
                JsonPrimitive(Jsoup.clean(element.content, safeList))
            } else {
                element
            }
        }
        else -> element
    }
}

fun getMentions(content: String?): Set<String> {
// (.*?) - capture any characters (non-greedy) into group 1
    return if(content != null) Regex(">@(.*?)</a>").find(content)?.groupValues?.toSet() ?: emptySet() else emptySet()
}

fun UserResponseDomain.validateUser(): Boolean =
    this.name?.replace("\\s+".toRegex(), "")?.length != 0

suspend inline fun <reified T> WebSocketServerSession.sendTypedMessage(messageType: MessageType, data: T) =
    sendSerialized<TypeMappingDomain<T>>(
        TypeMappingDomain(
            mapOf(messageType to data)
        )
    )

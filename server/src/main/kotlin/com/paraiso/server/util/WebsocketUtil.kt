package com.paraiso.server.util

import com.paraiso.domain.messageTypes.MessageType
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.nio.charset.Charset
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain

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

suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame
): T? =
    try {
        val cleanFrame = when (frame) {
            is Frame.Text -> {
                val rawText = frame.readText()

                val safeHtml = Jsoup.clean(
                    rawText,
                    safeList
                )

                Frame.Text(safeHtml)
            }
            else -> frame
        }

        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            frame
        ) as? T
    } catch (e: Exception) {
        println(e)
        null
    }

suspend inline fun <reified T> WebSocketServerSession.sendTypedMessage(messageType: MessageType, data: T) =
    sendSerialized<TypeMappingDomain<T>>(
        TypeMappingDomain(
            mapOf(messageType to data)
        )
    )

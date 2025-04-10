package com.paraiso.server.util

import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.nio.charset.Charset

val safeList: Safelist = Safelist()
    .addTags(
        "p", "br", "strong", "em", "u", "s", "code", "pre", "blockquote",
        "ul", "ol", "li", "a", "h1", "h2", "h3", "h4", "h5", "h6"
    )
    .addAttributes("a", "href", "target", "rel")
    .addProtocols("a", "href", "http", "https", "mailto")
    .preserveRelativeLinks(true)

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
            cleanFrame
        ) as? T
    } catch (e: Exception) {
        println(e)
        null
    }

suspend inline fun <reified T> WebSocketServerSession.sendTypedMessage(messageType: com.paraiso.server.messageTypes.MessageType, data: T) =
    sendSerialized<com.paraiso.server.messageTypes.TypeMapping<T>>(
        com.paraiso.server.messageTypes.TypeMapping(
            mapOf(messageType to data)
        )
    )

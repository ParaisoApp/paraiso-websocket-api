package com.paraiso.websocket.api.util

import com.paraiso.websocket.api.messageTypes.MessageType
import com.paraiso.websocket.api.messageTypes.TypeMapping
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import java.nio.charset.Charset
suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame
): T? =
    try {
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
    sendSerialized<TypeMapping<T>>(TypeMapping(mapOf(messageType to data)))

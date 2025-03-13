package com.example.util

import com.example.messageTypes.MessageType
import com.example.messageTypes.TypeMapping
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import java.nio.charset.Charset
suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame
): T? {
    return try {
        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            frame
        ) as? T
    } catch (e: Exception) {
        println(e)
        null
    }
}

suspend fun <T> WebSocketServerSession.sendTypedMessage(messageType: MessageType, data: T) {
    sendSerialized<TypeMapping<T>>(TypeMapping(mapOf(messageType to data)))
}

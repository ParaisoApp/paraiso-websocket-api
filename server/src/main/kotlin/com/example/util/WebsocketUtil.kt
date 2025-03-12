package com.example.util

import com.example.messageTypes.MessageType
import com.example.messageTypes.TypeMapping
import com.example.messageTypes.User
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

suspend inline fun <reified T> MutableMap<String, User>.broadcastToAllUsers(value: T, type: MessageType) {
    coroutineScope {
        this@broadcastToAllUsers.forEach { (_, user) ->
            launch {
                user.websocket.let {
                    user.websocket.sendSerialized<TypeMapping<T>>(TypeMapping(mapOf(type to value)))
                }
            }
        }
    }
}

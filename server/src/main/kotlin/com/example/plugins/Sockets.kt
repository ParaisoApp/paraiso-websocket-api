package com.example.plugins

import io.ktor.serialization.WebsocketContentConverter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.time.Duration
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

val allConnectedUsers: MutableMap<String, User> = Collections.synchronizedMap(LinkedHashMap())
// val groupChatUsers: MutableSet<User> = Collections.synchronizedSet(LinkedHashSet())

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(45)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("chat") {
            UUID.randomUUID().toString().let {id ->
                val currentUser = User(userId = id, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = this)
                allConnectedUsers[id] = currentUser
                joinGroupChat(currentUser)
            }
        }
    }
}
suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame
): T? {
    return try {
        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            frame
        ) as? T
    } catch (e: Exception){
        println(e)
        null
    }
}


suspend fun WebSocketServerSession.joinGroupChat(user: User) {
    sendSerialized<TypeMapping<List<String>>>(TypeMapping(mapOf(MessageType.USER_LIST to allConnectedUsers.values.map { it.userId })))
    sendSerialized<TypeMapping<String>>(TypeMapping(mapOf(MessageType.BASIC to "Happy Chatting")))
    allConnectedUsers.broadcastToAllUsers(user.userId, MessageType.GUEST, this)
    try {
        incoming.consumeEach { frame ->
            val messageWithType = converter?.findCorrectConversion<TypeMapping<String>>(frame)
                ?.typeMapping?.entries?.first()
            when(messageWithType?.key){
                MessageType.MSG -> {
                    val message = Json.decodeFromString<Message>(messageWithType.value).copy(
                        id = "${(Math.random() * 10000).toInt()}",
                        userId = user.userId
                    )
                    allConnectedUsers.broadcastToAllUsers(message, MessageType.MSG, this)
                }
                MessageType.VOTE -> {
                    val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = user.userId)
                    allConnectedUsers.broadcastToAllUsers(vote, MessageType.VOTE, this)
                }
                MessageType.DELETE -> {
                    val vote = Json.decodeFromString<Delete>(messageWithType.value).copy(userId = user.userId)
                    allConnectedUsers.broadcastToAllUsers(vote, MessageType.DELETE, this)
                }
                else -> println("error")
            }
        }
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        allConnectedUsers.broadcastToAllUsers("${user.username} disconnected", MessageType.BASIC, this)
        allConnectedUsers.remove(user.userId)
        user.websocket.close()
    }
}

fun getGreetingsText(users: MutableMap<String, User>, currentUserUsername: String): String {
    return if (users.count() == 1) {
        "You are the only one here"
    } else {
        """Welcome $currentUserUsername, There are ${users.count()} connected [${users.values.joinToString { it.username }}]""".trimMargin()
    }
}

suspend inline fun <reified T> MutableMap<String, User>.broadcastToAllUsers(value: T, type: MessageType, session: WebSocketServerSession) {
    coroutineScope {
        this@broadcastToAllUsers.forEach { (_, user) ->
            launch {
                session.converter?.let {
                    user.websocket.sendSerialized<TypeMapping<T>>(TypeMapping(mapOf(type to value)))
                }
            }
        }
    }
}

data class User(
    val userId: String,
    val username: String,
    val websocket: WebSocketServerSession,
    var isReadyToChat: Boolean = false
)

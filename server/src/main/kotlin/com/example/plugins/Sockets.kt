package com.example.plugins

import io.ktor.serialization.WebsocketContentConverter
import io.ktor.serialization.WebsocketConverterNotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.suitableCharset
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
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.time.Duration
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.reflect.typeOf

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
            UUID.randomUUID().toString().let {
                val currentUser = User(userId = it, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = this)
                allConnectedUsers[it] = currentUser
                joinGroupChat(currentUser)
            }
        }
    }
}
suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame
): Any? {
    val result = runCatching {
        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            frame
        )
    }
    return result.getOrNull()
}

suspend fun WebSocketServerSession.joinGroupChat(user: User) {
    sendSerialized("Happy Chatting.")
    try {
        incoming.consumeEach {frame ->
            val res = converter?.findCorrectConversion<Message>(frame) ?:
                converter?.findCorrectConversion<FooBar>(frame)
                println(res)
            when(res){
                is Message ->{
                    println(res)
//                    if(msg.type != MessageType.PONG){
//                        allConnectedUsers.broadcastToAllUsers(msg, this)
//                    }
                }
                else -> {println(res)}//ignore
            }
        }
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        allConnectedUsers.broadcastBasicToAllUsers("${user.username} disconnected", this)
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

suspend fun MutableMap<String, User>.broadcastToAllUsers(message: Message, session: WebSocketServerSession) {
    coroutineScope {
        this@broadcastToAllUsers.forEach { (_, user) ->
            launch{
                session.converter?.let{
                    user.websocket.sendSerialized(message)
                }
            }
        }
    }
}

suspend fun MutableMap<String, User>.broadcastBasicToAllUsers(message: String, session: WebSocketServerSession) {
    coroutineScope {
        this@broadcastBasicToAllUsers.forEach { (_, user) ->
            launch{
                session.converter?.let{
                    user.websocket.sendSerialized(message)
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

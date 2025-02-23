package com.example.plugins

import io.klogging.logger
import io.ktor.serialization.WebsocketConverterNotFoundException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.suitableCharset
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

val allConnectedUsers: MutableMap<String, User> = Collections.synchronizedMap(LinkedHashMap())
// val groupChatUsers: MutableSet<User> = Collections.synchronizedSet(LinkedHashSet())

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
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

suspend fun WebSocketServerSession.joinGroupChat(user: User) {
    allConnectedUsers.broadcastToAllUsers("${user.username} joined")
    send(Frame.Text("Happy Chatting."))
    try {
        incoming.consumeEach {frame ->
            when(frame){
                is Frame.Text ->{
                    try{
                        val mes = converter?.deserialize(
                            call.request.headers.suitableCharset(),
                            typeInfo<Message>(),
                            frame
                        ) as? Message
                        if(mes?.type != "pong"){
                            val outputMessage = "${user.username}: ${frame.readText()}"
                            allConnectedUsers.broadcastToAllUsers(outputMessage)
                        }
                    }catch(e: WebsocketConverterNotFoundException){
                        println(e.localizedMessage)
                    }
                }
                else -> {}//ignore
            }
        }
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        allConnectedUsers.broadcastToAllUsers("${user.username} disconnected")
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

suspend fun MutableMap<String, User>.broadcastToAllUsers(message: String) {
    this.forEach { (_, user) ->
        user.websocket.send(Frame.Text(message))
    }
}

data class User(
    val userId: String,
    val username: String,
    val websocket: DefaultWebSocketSession,
    var isReadyToChat: Boolean = false
)

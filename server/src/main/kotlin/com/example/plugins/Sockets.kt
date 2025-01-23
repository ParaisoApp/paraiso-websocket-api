package com.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.time.Duration
import java.util.Collections
import java.util.UUID

val allConnectedUsers: MutableSet<User> = Collections.synchronizedSet(LinkedHashSet())
val groupChatUsers: MutableSet<User> = Collections.synchronizedSet(LinkedHashSet())

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("chat") {
            send(Frame.Text("Enter Your name:"))
            val username = (incoming.receive() as Frame.Text).readText().ifEmpty { "Guest ${UUID.randomUUID()}" }
            val currentLoggedInUser = User(username = username, websocket = this)
            allConnectedUsers += currentLoggedInUser
            val greetingText = Frame.Text(getGreetingsText(allConnectedUsers, username))
            send(greetingText)
            send(Frame.Text("If you would like to join group chat type 1 or 0 if you want to talk privately with connected users"))
            val roomResponse = (incoming.receive() as Frame.Text).readText()
            when (roomResponse) {
                "1" -> {
                    sendCurrentUserToGroupChat(currentLoggedInUser)
                }
                "0" -> listConnectedUsersAndConnect(currentLoggedInUser)
                else -> {
                    send(Frame.Text("Well this is not an option, so you'll be redirected to group chat"))
                    sendCurrentUserToGroupChat(currentLoggedInUser)
                }
            }
        }
    }
}

suspend fun DefaultWebSocketSession.listConnectedUsersAndConnect(currentUser: User) {
    send(
        Frame.Text(
            """Here are the currently connected users
            Choose one to chat with
            ${allConnectedUsers.mapIndexed { index, user -> "$index-${user.username}${if (groupChatUsers.contains(user)) "(in group chat)" else ""}" }}
            NOTE: The user you'll choose will receive messages from you whether if they was
            in group or private channel, but they can't text you back unless they chose to
            """.trimMargin()
        )
    )
    val chatMateResponse = (incoming.receive() as Frame.Text).readText()
    val chatMate: User? = try {
        allConnectedUsers.toList()[chatMateResponse.toInt()]
    } catch (e: Exception) {
        send(Frame.Text("Specified user was not found or may have disconnected"))
        null
    }
    if (chatMate == null) {
        send(Frame.Text("You'll be sent to group chat and will notify once someone connects"))
        return sendCurrentUserToGroupChat(currentUser)
    }
    try {
        send(Frame.Text("Happy chatting with ${chatMate.username}"))
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val outputMessage = "${currentUser.username}: ${frame.readText()}"
                chatMate.websocket.send(Frame.Text(outputMessage))
            }
        }
    } catch (e: NumberFormatException) {
        send(Frame.Text("No Such user were found, thus you'll be redirected to group channel"))
        sendCurrentUserToGroupChat(currentUser)
    } catch (e: IndexOutOfBoundsException) {
        send(Frame.Text("No Such user were found, thus you'll be redirected to group channel"))
        sendCurrentUserToGroupChat(currentUser)
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        chatMate.websocket.send(Frame.Text("${currentUser.username} disconnected"))
        allConnectedUsers -= currentUser
        currentUser.websocket.close()
    }
}

suspend fun DefaultWebSocketSession.joinGroupChat(currentUser: User) {
    send(Frame.Text("Happy Chatting."))
    try {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val outputMessage = "${currentUser.username}: ${frame.readText()}"
                groupChatUsers.forEach { user ->
                    user.websocket.send(Frame.Text(outputMessage))
                }
            }
        }
    } catch (e: Exception) {
        println(e.localizedMessage)
    } finally {
        groupChatUsers.broadcastToAllUsers("${currentUser.username} disconnected")
        allConnectedUsers -= currentUser
        groupChatUsers -= currentUser
        currentUser.websocket.close()
    }
}

fun getGreetingsText(users: MutableSet<User>, currentUserUsername: String): String {
    return if (users.count() == 1) {
        "You are the only one here"
    } else {
        """Welcome $currentUserUsername, There are ${users.count()} connected [${users.joinToString { it.username }}]""".trimMargin()
    }
}

suspend fun MutableSet<User>.broadcastToAllUsers(message: String) {
    this.forEach { user ->
        user.websocket.send(Frame.Text(message))
    }
}

suspend fun DefaultWebSocketSession.sendCurrentUserToGroupChat(currentLoggedInUser: User) {
    groupChatUsers.broadcastToAllUsers("${currentLoggedInUser.username} joined")
    groupChatUsers += currentLoggedInUser
    joinGroupChat(currentLoggedInUser)
}

data class User(
    val userId: String = UUID.randomUUID().toString(),
    val username: String,
    val websocket: DefaultWebSocketSession,
    var isReadyToChat: Boolean = false
)

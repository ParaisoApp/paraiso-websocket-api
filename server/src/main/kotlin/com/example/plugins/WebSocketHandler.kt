package com.example.plugins

import com.example.messageTypes.BoxScore
import com.example.messageTypes.Delete
import com.example.messageTypes.Message
import com.example.messageTypes.MessageType
import com.example.messageTypes.Scoreboard
import com.example.messageTypes.TypeMapping
import com.example.messageTypes.User
import com.example.messageTypes.Vote
import com.example.testRestClient.sport.SportOperationAdapter
import com.example.util.broadcastToAllUsers
import com.example.util.findCorrectConversion
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler: Klogging {
    private var scoreboard: Scoreboard? = null
    private var boxScores: List<BoxScore>? = null
    suspend fun buildScoreboard(){
        val sportAdapter = SportOperationAdapter()
        scoreboard = sportAdapter.getSchedule()
        updateScores(sportAdapter)
    }

    suspend fun updateScores(sportAdapter: SportOperationAdapter){
        scoreboard?.competitions?.map {
            delay(1000L)
            sportAdapter.getGameStats(it.id)
        }?.filterNotNull()?.also {
            boxScores = it
        }
        delay(300000L)
        updateScores(sportAdapter)
    }

    suspend fun handleGuest(session: WebSocketServerSession, allConnectedUsers: MutableMap<String, User>){
        UUID.randomUUID().toString().let { id ->
            val currentUser = User(userId = id, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = session)
            allConnectedUsers[id] = currentUser
            session.joinGroupChat(currentUser, allConnectedUsers)
        }
    }
    private suspend fun WebSocketServerSession.joinGroupChat(user: User, allConnectedUsers: MutableMap<String, User>) {
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
                    else -> logger.error{"Invalid message type received $messageWithType"}
                }
            }
        } catch (ex: Exception) {
            logger.error(ex){"Error parsing incoming data"}
        } finally {
            allConnectedUsers.broadcastToAllUsers("${user.username} disconnected", MessageType.BASIC, this)
            allConnectedUsers.remove(user.userId)
            user.websocket.close()
        }
    }
}
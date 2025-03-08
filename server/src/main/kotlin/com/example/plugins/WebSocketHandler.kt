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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler: Klogging {
    private var scoreboard: Scoreboard? = null
    private var boxScores: List<BoxScore> = listOf()
    private val allConnectedUsers: MutableMap<String, User> = mutableMapOf()
    suspend fun buildScoreboard(){
        val sportAdapter = SportOperationAdapter()
        scoreboard = sportAdapter.getSchedule()
        //updateScores(sportAdapter)
    }

    private suspend fun updateScores(sportAdapter: SportOperationAdapter){
        scoreboard?.competitions?.mapNotNull {
            delay(10000L)
            sportAdapter.getGameStats(it.id)
        }?.also { newBoxScores ->
            allConnectedUsers.broadcastToAllUsers(boxScores, MessageType.BOX_SCORES)
            boxScores = newBoxScores
        }
        delay(300000L)
        updateScores(sportAdapter)
    }

    suspend fun handleGuest(session: WebSocketServerSession){
        UUID.randomUUID().toString().let { id ->
            val currentUser = User(userId = id, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = session)
            allConnectedUsers[id] = currentUser
            session.joinGroupChat(currentUser)
        }
    }
    private suspend fun WebSocketServerSession.joinGroupChat(user: User) {
        launch {
            while(true){
                scoreboard?.let {
                    sendSerialized<TypeMapping<Scoreboard?>>(TypeMapping(mapOf(MessageType.SCOREBOARD to scoreboard)))
                }
                delay(50000L)
            }
        }
        sendSerialized<TypeMapping<List<String>>>(TypeMapping(mapOf(MessageType.USER_LIST to allConnectedUsers.values.map { it.userId })))
        sendSerialized<TypeMapping<String>>(TypeMapping(mapOf(MessageType.BASIC to "Happy Chatting")))
        allConnectedUsers.broadcastToAllUsers(user.userId, MessageType.GUEST)
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
                        allConnectedUsers.broadcastToAllUsers(message, MessageType.MSG)
                    }
                    MessageType.VOTE -> {
                        val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = user.userId)
                        allConnectedUsers.broadcastToAllUsers(vote, MessageType.VOTE)
                    }
                    MessageType.DELETE -> {
                        val vote = Json.decodeFromString<Delete>(messageWithType.value).copy(userId = user.userId)
                        allConnectedUsers.broadcastToAllUsers(vote, MessageType.DELETE)
                    }
                    else -> logger.error{"Invalid message type received $messageWithType"}
                }
            }
        } catch (ex: Exception) {
            logger.error(ex){"Error parsing incoming data"}
        } finally {
            allConnectedUsers.broadcastToAllUsers("${user.username} disconnected", MessageType.BASIC)
            allConnectedUsers.remove(user.userId)
            user.websocket.close()
        }
    }
}
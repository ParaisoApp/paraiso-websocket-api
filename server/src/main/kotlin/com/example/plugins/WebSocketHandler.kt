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
import com.example.testRestClient.util.ApiConfig
import com.example.util.findCorrectConversion
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler : Klogging {
    private var scoreboard: Scoreboard? = null
    private var boxScores: List<BoxScore> = listOf()
    private val allConnectedUsers: MutableMap<String, User> = mutableMapOf()

    private val messageSharedFlowMut = MutableSharedFlow<Message>(replay = 0)
    private val messageSharedFlow = messageSharedFlowMut.asSharedFlow()

    private val voteSharedFlowMut = MutableSharedFlow<Vote>(replay = 0)
    private val voteSharedFlow = voteSharedFlowMut.asSharedFlow()

    private val deleteSharedFlowMut = MutableSharedFlow<Delete>(replay = 0)
    private val deleteSharedFlow = deleteSharedFlowMut.asSharedFlow()

    private val basicSharedFlowMut = MutableSharedFlow<String>(replay = 0)
    private val basicSharedFlow = basicSharedFlowMut.asSharedFlow()

    private val boxScoreFlowMut = MutableSharedFlow<List<BoxScore>>(replay = 0)
    private val boxScoreFlow = boxScoreFlowMut.asSharedFlow()
    suspend fun buildScoreboard() {
        val apiConfig = ApiConfig()
        val sportAdapter = SportOperationAdapter(apiConfig)
        scoreboard = sportAdapter.getSchedule()
        // updateScores(sportAdapter)
    }

    private suspend fun updateScores(sportAdapter: SportOperationAdapter) {
        scoreboard?.competitions?.mapNotNull {
            delay(10000L)
            sportAdapter.getGameStats(it.id)
        }?.also { newBoxScores ->
            boxScoreFlowMut.emit(newBoxScores)
            boxScores = newBoxScores
        }
        delay(300000L)
        updateScores(sportAdapter)
    }

    suspend fun handleGuest(session: WebSocketServerSession) {
        UUID.randomUUID().toString().let { id ->
            val currentUser = User(userId = id, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = session)
            allConnectedUsers[id] = currentUser
            session.joinGroupChat(currentUser)
        }
    }
    private suspend fun WebSocketServerSession.joinGroupChat(user: User) {
        val sendScoreboard = launch {
            while (true) {
                scoreboard?.let {
                    sendSerialized<TypeMapping<Scoreboard?>>(TypeMapping(mapOf(MessageType.SCOREBOARD to scoreboard)))
                }
                delay(50000L)
            }
        }
        sendSerialized<TypeMapping<List<String>>>(TypeMapping(mapOf(MessageType.USER_LIST to allConnectedUsers.values.map { it.userId })))
        sendSerialized<TypeMapping<String>>(TypeMapping(mapOf(MessageType.BASIC to "Happy Chatting")))

        basicSharedFlowMut.emit(user.userId)
        val jobs = listOf(
            launch {
                messageSharedFlow.collect { message ->
                    sendSerialized<TypeMapping<Message>>(TypeMapping(mapOf(MessageType.MSG to message)))
                }
            },
            launch {
                voteSharedFlow.collect { message ->
                    sendSerialized<TypeMapping<Vote>>(TypeMapping(mapOf(MessageType.VOTE to message)))
                }
            },
            launch {
                deleteSharedFlow.collect { message ->
                    sendSerialized<TypeMapping<Delete>>(TypeMapping(mapOf(MessageType.DELETE to message)))
                }
            },
            launch {
                basicSharedFlow.collect { message ->
                    sendSerialized<TypeMapping<String>>(TypeMapping(mapOf(MessageType.GUEST to message)))
                }
            },
            launch {
                boxScoreFlow.collect { message ->
                    sendSerialized<TypeMapping<List<BoxScore>>>(TypeMapping(mapOf(MessageType.BOX_SCORES to message)))
                }
            }
        )

        runCatching {
                incoming.consumeEach { frame ->
                    val messageWithType = converter?.findCorrectConversion<TypeMapping<String>>(frame)
                        ?.typeMapping?.entries?.first()
                    when (messageWithType?.key) {
                        MessageType.MSG -> {
                            val message = Json.decodeFromString<Message>(messageWithType.value).copy(
                                id = "${(Math.random() * 10000).toInt()}",
                                userId = user.userId
                            )
                            messageSharedFlowMut.emit(message)
                        }
                        MessageType.VOTE -> {
                            val vote = Json.decodeFromString<Vote>(messageWithType.value).copy(userId = user.userId)
                            voteSharedFlowMut.emit(vote)
                        }
                        MessageType.DELETE -> {
                            val delete = Json.decodeFromString<Delete>(messageWithType.value).copy(userId = user.userId)
                            deleteSharedFlowMut.emit(delete)
                        }
                        else -> logger.error { "Invalid message type received $messageWithType" }
                    }
                }
        }.onFailure{ ex ->
            logger.error(ex) { "Error parsing incoming data" }
        }.also {
            jobs.forEach { it.cancelAndJoin() }
            sendScoreboard.cancelAndJoin()
            basicSharedFlowMut.emit("${user.username} disconnected")
            allConnectedUsers.remove(user.userId)
            user.websocket.close()
        }
    }
}

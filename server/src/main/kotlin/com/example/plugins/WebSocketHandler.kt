package com.example.plugins

import com.example.messageTypes.sports.BoxScore
import com.example.messageTypes.Delete
import com.example.messageTypes.DirectMessage
import com.example.messageTypes.Message
import com.example.messageTypes.MessageType
import com.example.messageTypes.sports.Scoreboard
import com.example.messageTypes.TypeMapping
import com.example.messageTypes.User
import com.example.messageTypes.UserInfo
import com.example.messageTypes.Vote
import com.example.messageTypes.sports.AllStandings
import com.example.messageTypes.sports.Team
import com.example.testRestClient.sport.SportOperationAdapter
import com.example.testRestClient.util.ApiConfig
import com.example.util.findCorrectConversion
import com.example.util.sendTypedMessage
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.converter
import io.ktor.websocket.close
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketHandler(private val sportHandler: SportHandler) : Klogging {

    private val userList: MutableMap<String, User> = mutableMapOf()

    private val messageSharedFlowMut = MutableSharedFlow<Message>(replay = 0)
    private val voteSharedFlowMut = MutableSharedFlow<Vote>(replay = 0)
    private val deleteSharedFlowMut = MutableSharedFlow<Delete>(replay = 0)
    private val basicSharedFlowMut = MutableSharedFlow<String>(replay = 0)
    private val guestSharedFlowMut = MutableSharedFlow<UserInfo>(replay = 0)
    private val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)

    private val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageSharedFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteSharedFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteSharedFlowMut.asSharedFlow()),
        Pair(MessageType.BASIC, basicSharedFlowMut.asSharedFlow()),
        Pair(MessageType.GUEST, guestSharedFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LEAVE, userLeaveFlowMut.asSharedFlow()),
    )

    suspend fun handleGuest(session: WebSocketServerSession) {
        UUID.randomUUID().toString().let { id ->
            val currentUser = User(userId = id, username = "Guest ${(Math.random() * 10000).toInt()}", websocket = session)
            userList[id] = currentUser
            session.joinChat(currentUser)
        }
    }
    private suspend fun WebSocketServerSession.joinChat(user: User) {
        sendTypedMessage(MessageType.USER_LIST, userList.values.map { UserInfo(it.userId, it.username) })
        sendTypedMessage(MessageType.BASIC, "Happy Chatting")

        val messageCollectionJobs = flowList.map { (type, sharedFlow) ->
            launch {
                sharedFlow.collect { message ->
                    when (type) {
                        MessageType.MSG -> sendTypedMessage(type, message as Message)
                        MessageType.VOTE -> sendTypedMessage(type, message as Vote)
                        MessageType.DELETE -> sendTypedMessage(type, message as Delete)
                        MessageType.BASIC -> sendTypedMessage(type, message as String)
                        MessageType.GUEST -> sendTypedMessage(type, message as UserInfo)
                        MessageType.USER_LEAVE -> sendTypedMessage(type, message as String)
                        else -> logger.error { "Found unknown type when sending typed message from flow $sharedFlow" }
                    }
                }
            }
        }

        val statsJobs = listOf(
            launch {
                while (true) {
                    sportHandler.scoreboard?.let {
                        sendTypedMessage(MessageType.SCOREBOARD, it)
                        delay(500000L)
                    } ?: run { delay(5000L) }

                }
            },
            launch {
                while (true) {
                    if(sportHandler.teams.isNotEmpty()){
                        sendTypedMessage(MessageType.TEAMS, sportHandler.teams)
                        delay(500000L)
                    }else{
                        delay(5000L)
                    }
                }
            },
            launch {
                while (true) {
                    sportHandler.standings?.let {
                        sendTypedMessage(MessageType.STANDINGS, it)
                        delay(5000000L)
                    } ?: run { delay(5000L) }
                }
            },
            launch {
                while (true) {
                    if(sportHandler.boxScores.isNotEmpty()){
                        sendTypedMessage(MessageType.BOX_SCORES, sportHandler.boxScores)
                    }
                    delay(50000L)
                }
            },
            launch {
                sportHandler.boxScoreFlowMut.collect { message ->
                    sendTypedMessage(MessageType.BOX_SCORES, message)
                }
            }
        )

        guestSharedFlowMut.emit(UserInfo(user.userId, user.username))

        try {
            incoming.consumeEach { frame ->
                val messageWithType = converter?.findCorrectConversion<TypeMapping<String>>(frame)
                    ?.typeMapping?.entries?.first()
                when (messageWithType?.key) {
                    MessageType.MSG -> {
                        val message = Json.decodeFromString<Message>(messageWithType.value).copy(
                            id = UUID.randomUUID().toString(),
                            userId = user.userId
                        )
                        messageSharedFlowMut.emit(message)
                    }
                    MessageType.DM -> {
                        val message = Json.decodeFromString<DirectMessage>(messageWithType.value).copy(
                            id = UUID.randomUUID().toString(),
                            userId = user.userId
                        )
                        launch { sendTypedMessage(MessageType.DM, message) }
                        userList[message.userReceiveId]?.websocket?.sendTypedMessage(MessageType.DM, message)
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
        }catch(ex: Exception){
            logger.error(ex) { "Error parsing incoming data" }
        }finally {
            messageCollectionJobs.forEach { it.cancelAndJoin() }
            statsJobs.forEach { it.cancelAndJoin() }
            userLeaveFlowMut.emit(user.userId)
            userList.remove(user.userId)
            user.websocket.close()
        }
    }
}

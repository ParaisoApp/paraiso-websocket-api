package com.paraiso.com.paraiso.server.plugins.jobs

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.util.ServerState
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.util.sendTypedMessage
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import reactor.core.Disposable
import java.util.concurrent.ConcurrentHashMap

class MessageHandler(
    private val serverId: String,
    private val userSessions: ConcurrentHashMap<String, Set<WebSocketServerSession>>,
    private val eventServiceImpl: EventServiceImpl,
): Klogging {
    suspend fun messageJobs() = coroutineScope {
        //pick up dms directed at this server - find active user and send typed message
        launch{
            eventServiceImpl.subscribe("server:$serverId") { message ->
                val (userId, payload) = message.split(":", limit = 2)
                try {
                    val dm = Json.decodeFromString<DirectMessage>(payload)
                    userSessions[userId]?.forEach { session ->
                        session.sendTypedMessage(MessageType.DM, dm)
                    }
                } catch (e: SerializationException) {
                    logger.error(e) { "Error deserializing: $payload" }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.USER_UPDATE.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                //only emit messages coming from other servers
                if(incomingServerId != serverId){
                    try {
                        val userUpdate = Json.decodeFromString<UserResponse>(payload)
                        ServerState.userUpdateFlowMut.emit(userUpdate)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.MSG.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val parsedMessage = Json.decodeFromString<Message>(payload)
                        ServerState.messageFlowMut.emit(parsedMessage)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.FOLLOW.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val follow = Json.decodeFromString<Follow>(payload)
                        ServerState.followFlowMut.emit(follow)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.FAVORITE.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val favorite = Json.decodeFromString<Favorite>(payload)
                        ServerState.favoriteFlowMut.emit(favorite)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.VOTE.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val vote = Json.decodeFromString<Vote>(payload)
                        ServerState.voteFlowMut.emit(vote)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.DELETE.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val delete = Json.decodeFromString<Delete>(payload)
                        ServerState.deleteFlowMut.emit(delete)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.BAN.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val ban = Json.decodeFromString<Ban>(payload)
                        ServerState.banUserFlowMut.emit(ban)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.TAG.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val tag = Json.decodeFromString<Tag>(payload)
                        ServerState.tagUserFlowMut.emit(tag)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.REPORT_USER.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val reportUser = Json.decodeFromString<Report>(payload)
                        ServerState.reportUserFlowMut.emit(reportUser)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.REPORT_POST.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val reportPost = Json.decodeFromString<Report>(payload)
                        ServerState.reportPostFlowMut.emit(reportPost)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
        launch{
            eventServiceImpl.subscribe("${MessageType.REPORT_POST.name}:") { message ->
                val (incomingServerId, payload) = message.split(":", limit = 2)
                if(incomingServerId != serverId){
                    try {
                        val reportPost = Json.decodeFromString<Report>(payload)
                        ServerState.reportPostFlowMut.emit(reportPost)
                    } catch (e: SerializationException) {
                        logger.error(e) { "Error deserializing: $payload" }
                    }
                }
            }
        }
    }
}
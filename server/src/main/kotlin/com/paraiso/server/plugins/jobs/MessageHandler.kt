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
import com.paraiso.server.util.decodeMessage
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
        eventServiceImpl.addChannels(
            listOf(
                "server:$serverId",
                MessageType.USER_UPDATE.name,
                MessageType.MSG.name,
                MessageType.FOLLOW.name,
                MessageType.FAVORITE.name,
                MessageType.VOTE.name,
                MessageType.DELETE.name,
                MessageType.BAN.name,
                MessageType.TAG.name,
                MessageType.REPORT_USER.name,
                MessageType.REPORT_POST.name,
            )
        )
        //pick up dms directed at this server - find active user and send typed message
        launch{
            eventServiceImpl.subscribe { (channel, message) ->
                when(channel){
                    "server:$serverId" -> {
                        val (userId, payload) = message.split(":", limit = 2)
                        decodeMessage<DirectMessage>(payload)?.let {dm ->
                            userSessions[userId]?.forEach { session ->
                                session.sendTypedMessage(MessageType.DM, dm)
                            }
                        }
                    }
                    MessageType.USER_UPDATE.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        //only emit messages coming from other servers
                        if(incomingServerId != serverId){
                            decodeMessage<UserResponse>(payload)?.let {userUpdate ->
                                ServerState.userUpdateFlowMut.emit(userUpdate)
                            }
                        }
                    }
                    MessageType.MSG.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Message>(payload)?.let {parsedMessage ->
                                ServerState.messageFlowMut.emit(parsedMessage)
                            }
                        }
                    }
                    MessageType.FOLLOW.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Follow>(payload)?.let {follow ->
                                ServerState.followFlowMut.emit(follow)
                            }
                        }
                    }
                    MessageType.FAVORITE.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Favorite>(payload)?.let {favorite ->
                                ServerState.favoriteFlowMut.emit(favorite)
                            }
                        }
                    }
                    MessageType.VOTE.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Vote>(payload)?.let {vote ->
                                ServerState.voteFlowMut.emit(vote)
                            }
                        }
                    }
                    MessageType.DELETE.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Delete>(payload)?.let {delete ->
                                ServerState.deleteFlowMut.emit(delete)
                            }
                        }
                    }
                    MessageType.BAN.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Ban>(payload)?.let {ban ->
                                ServerState.banUserFlowMut.emit(ban)
                            }
                        }
                    }
                    MessageType.TAG.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Tag>(payload)?.let {tag ->
                                ServerState.tagUserFlowMut.emit(tag)
                            }
                        }
                    }
                    MessageType.REPORT_USER.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Report>(payload)?.let {reportUser ->
                                ServerState.reportUserFlowMut.emit(reportUser)
                            }
                        }
                    }
                    MessageType.REPORT_POST.name -> {
                        val (incomingServerId, payload) = message.split(":", limit = 2)
                        if(incomingServerId != serverId){
                            decodeMessage<Report>(payload)?.let {reportPost ->
                                ServerState.reportPostFlowMut.emit(reportPost)
                            }
                        }
                    }
                }
            }
        }
    }
}
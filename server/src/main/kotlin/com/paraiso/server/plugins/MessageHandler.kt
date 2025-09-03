package com.paraiso.server.plugins

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
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.sports.SportState
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.util.ServerState
import com.paraiso.events.EventServiceImpl
import com.paraiso.server.util.decodeMessage
import com.paraiso.server.util.sendTypedMessage
import io.klogging.Klogging
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
                MessageType.SCOREBOARD.name,
                MessageType.BOX_SCORES.name,
            )
        )
        //pick up dms directed at this server - find active user and send typed message
        launch{
            eventServiceImpl.subscribe { (channel, messageWithServer) ->
                val (incomingModifier, message) = messageWithServer.split(":", limit = 2)
                //only emit messages coming from other servers
                if(incomingModifier != serverId) {
                    when (channel) {
                        "server:$serverId" -> {
                            val (userId, payload) = message.split(":", limit = 2)
                            decodeMessage<DirectMessage>(payload)?.let { dm ->
                                userSessions[userId]?.forEach { session ->
                                    session.sendTypedMessage(MessageType.DM, dm)
                                }
                            }
                        }
                        MessageType.USER_UPDATE.name -> {
                            decodeMessage<UserResponse>(message)?.let { userUpdate ->
                                ServerState.userUpdateFlowMut.emit(userUpdate)
                            }
                        }
                        MessageType.MSG.name -> {
                            decodeMessage<Message>(message)?.let { parsedMessage ->
                                ServerState.messageFlowMut.emit(parsedMessage)
                            }
                        }

                        MessageType.FOLLOW.name -> {
                            decodeMessage<Follow>(message)?.let { follow ->
                                ServerState.followFlowMut.emit(follow)
                            }
                        }

                        MessageType.FAVORITE.name -> {
                            decodeMessage<Favorite>(message)?.let { favorite ->
                                ServerState.favoriteFlowMut.emit(favorite)
                            }
                        }

                        MessageType.VOTE.name -> {
                            decodeMessage<Vote>(message)?.let { vote ->
                                ServerState.voteFlowMut.emit(vote)
                            }
                        }

                        MessageType.DELETE.name -> {
                            decodeMessage<Delete>(message)?.let { delete ->
                                ServerState.deleteFlowMut.emit(delete)
                            }
                        }

                        MessageType.BAN.name -> {
                            decodeMessage<Ban>(message)?.let { ban ->
                                ServerState.banUserFlowMut.emit(ban)
                            }
                        }

                        MessageType.TAG.name -> {
                            decodeMessage<Tag>(message)?.let { tag ->
                                ServerState.tagUserFlowMut.emit(tag)
                            }
                        }

                        MessageType.REPORT_USER.name -> {
                            decodeMessage<Report>(message)?.let { reportUser ->
                                ServerState.reportUserFlowMut.emit(reportUser)
                            }
                        }

                        MessageType.REPORT_POST.name -> {
                            decodeMessage<Report>(message)?.let { reportPost ->
                                ServerState.reportPostFlowMut.emit(reportPost)
                            }
                        }

                        MessageType.SCOREBOARD.name -> {
                            decodeMessage<Scoreboard>(message)?.let { scoreboard ->
                                SportState.updateScoreboard(incomingModifier, scoreboard)
                            }
                        }

                        MessageType.BOX_SCORES.name -> {
                            decodeMessage<List<BoxScore>>(message)?.let { boxScores ->
                                SportState.updateBoxscore(incomingModifier, boxScores)
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.paraiso.server.plugins

import com.paraiso.domain.follows.Follow
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.messageTypes.ServerEvent
import com.paraiso.domain.messageTypes.SubscriptionInfo
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.TypeMapping
import com.paraiso.domain.messageTypes.toSubscription
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.ScoreboardBasic
import com.paraiso.domain.sport.sports.SportState
import com.paraiso.domain.userchats.DirectMessage
import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.User
import com.paraiso.domain.util.ServerState
import com.paraiso.domain.votes.Vote
import com.paraiso.server.util.decodeMessage
import com.paraiso.server.util.sendTypedMessage
import io.klogging.Klogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class MessageHandler(
    private val serverId: String,
    private val userSessions: ConcurrentHashMap<String, ConcurrentHashMap<String, SessionContext>>,
    private val eventService: EventService
) : Klogging {
    suspend fun messageJobs() = coroutineScope {
        eventService.addChannels(
            listOf(
                "server:$serverId",
                MessageType.USER_UPDATE.name,
                MessageType.MSG.name,
                MessageType.FOLLOW.name,
                MessageType.FAVORITE.name,
                MessageType.VOTE.name,
                MessageType.DELETE.name,
                MessageType.ROLE_UPDATE.name,
                MessageType.BAN.name,
                MessageType.TAG.name,
                MessageType.REPORT_USER.name,
                MessageType.REPORT_POST.name,
                MessageType.SCOREBOARD.name,
                MessageType.COMPS.name,
                MessageType.BOX_SCORES.name
            )
        )
        // pick up dms directed at this server - find active user and send typed message
        launch {
            eventService.subscribe { (channel, messageWithServer) ->
                val (incomingModifier, message) = messageWithServer.split(":", limit = 2)
                // modifier indicates server source on generic sends - otherwise channel indicates which server message is aimed to
                if (incomingModifier != serverId) {
                    when (channel) {
                        "server:$serverId" -> {
                            val (type, payload) = message.split(":", limit = 2)
                            when (type) {
                                MessageType.SUBSCRIBE.name -> {
                                    decodeMessage<SubscriptionInfo>(payload)?.let { subscriptionInfo ->
                                        val typeMapping = TypeMapping(typeMapping = mapOf(MessageType.SUBSCRIBE to subscriptionInfo.toSubscription()))
                                        userSessions[subscriptionInfo.userId]?.get(subscriptionInfo.sessionId)?.inboundChannel?.trySend(Json.encodeToString(typeMapping))
                                    }
                                }
                                MessageType.DM.name -> {
                                    decodeMessage<DirectMessage>(payload)?.let { dm ->
                                        userSessions[dm.userReceiveId]?.map { it.value.session }?.forEach { session ->
                                            session.sendTypedMessage(MessageType.DM, dm)
                                        }
                                    }
                                }
                            }
                        }
                        MessageType.USER_UPDATE.name -> {
                            decodeMessage<User>(message)?.let { userUpdate ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.UserUpdateReceived(userUpdate))
                            }
                        }
                        MessageType.MSG.name -> {
                            decodeMessage<Message>(message)?.let { parsedMessage ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.MessageReceived(parsedMessage))
                            }
                        }

                        MessageType.FOLLOW.name -> {
                            decodeMessage<Follow>(message)?.let { follow ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.FollowReceived(follow))
                            }
                        }

                        MessageType.FAVORITE.name -> {
                            decodeMessage<Favorite>(message)?.let { favorite ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.FavoriteReceived(favorite))
                            }
                        }

                        MessageType.VOTE.name -> {
                            decodeMessage<Vote>(message)?.let { vote ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.VoteReceived(vote))
                            }
                        }

                        MessageType.DELETE.name -> {
                            decodeMessage<Delete>(message)?.let { delete ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.DeleteReceived(delete))
                            }
                        }

                        MessageType.PIN_POST.name -> {
                            decodeMessage<PostPin>(message)?.let { postPin ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.PostPinReceived(postPin))
                            }
                        }

                        MessageType.ROLE_UPDATE.name -> {
                            decodeMessage<RoleUpdate>(message)?.let { roleUpdate ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.RoleUpdateReceived(roleUpdate))
                            }
                        }

                        MessageType.BAN.name -> {
                            decodeMessage<Ban>(message)?.let { ban ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.BanReceived(ban))
                            }
                        }

                        MessageType.TAG.name -> {
                            decodeMessage<Tag>(message)?.let { tag ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.TagReceived(tag))
                            }
                        }

                        MessageType.REPORT_USER.name -> {
                            decodeMessage<Report>(message)?.let { reportUser ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.UserReportReceived(reportUser))
                            }
                        }

                        MessageType.REPORT_POST.name -> {
                            decodeMessage<Report>(message)?.let { reportPost ->
                                ServerState.eventReceivedFlowMut.emit(ServerEvent.PostReportReceived(reportPost))
                            }
                        }

                        MessageType.SCOREBOARD.name -> {
                            decodeMessage<ScoreboardBasic>(message)?.let { scoreboard ->
                                SportState.updateScoreboard(incomingModifier, scoreboard)
                            }
                        }

                        MessageType.COMPS.name -> {
                            decodeMessage<List<Competition>>(message)?.let { comps ->
                                SportState.updateCompetitions(comps)
                            }
                        }

                        MessageType.BOX_SCORES.name -> {
                            decodeMessage<List<BoxScore>>(message)?.let { boxScores ->
                                SportState.updateBoxScores(boxScores)
                            }
                        }
                    }
                }
            }
        }
    }
}

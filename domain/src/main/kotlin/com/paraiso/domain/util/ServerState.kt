package com.paraiso.domain.util

import com.paraiso.domain.follows.Follow
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.messageTypes.RouteUpdate
import com.paraiso.domain.messageTypes.ServerEvent
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.users.User
import com.paraiso.domain.votes.Vote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ServerState {
    val eventReceivedFlowMut = MutableSharedFlow<ServerEvent>(replay = 0)
    val eventReceivedFlow = eventReceivedFlowMut.asSharedFlow()
}

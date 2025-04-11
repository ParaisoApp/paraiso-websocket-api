package com.paraiso.domain.util

import com.paraiso.domain.auth.User
import com.paraiso.domain.util.messageTypes.Ban
import com.paraiso.domain.util.messageTypes.Delete
import com.paraiso.domain.util.messageTypes.Message
import com.paraiso.domain.util.messageTypes.MessageType
import com.paraiso.domain.util.messageTypes.Vote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ServerState {
    val userList: MutableMap<String, User> = mutableMapOf()
    val banList: MutableSet<String> = mutableSetOf()

    val messageFlowMut = MutableSharedFlow<Message>(replay = 0)
    val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    val deleteFlowMut = MutableSharedFlow<Delete>(replay = 0)
    val userLoginFlowMut = MutableSharedFlow<User>(replay = 0)
    val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)
    val banUserFlowMut = MutableSharedFlow<Ban>(replay = 0)

    val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LOGIN, userLoginFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LEAVE, userLeaveFlowMut.asSharedFlow()),
        Pair(MessageType.BAN, banUserFlowMut.asSharedFlow())
    )
}

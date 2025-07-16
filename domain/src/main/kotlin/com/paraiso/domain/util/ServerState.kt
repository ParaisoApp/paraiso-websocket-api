package com.paraiso.domain.util

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.posts.Post
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

object ServerState {
    val userList: ConcurrentHashMap<String, User> = ConcurrentHashMap()
    val banList: MutableSet<String> = mutableSetOf()

    val posts: ConcurrentHashMap<String, Post> = ConcurrentHashMap()
    val sportPosts: ConcurrentHashMap<String, Post> = ConcurrentHashMap()

    val messageFlowMut = MutableSharedFlow<Message>(replay = 0)
    val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    val followFlowMut = MutableSharedFlow<Follow>(replay = 0)
    val deleteFlowMut = MutableSharedFlow<Delete>(replay = 0)
    val userLoginFlowMut = MutableSharedFlow<UserResponse>(replay = 0)
    val userLeaveFlowMut = MutableSharedFlow<String>(replay = 0)
    val banUserFlowMut = MutableSharedFlow<Ban>(replay = 0)

    val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteFlowMut.asSharedFlow()),
        Pair(MessageType.FOLLOW, followFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LOGIN, userLoginFlowMut.asSharedFlow()),
        Pair(MessageType.USER_LEAVE, userLeaveFlowMut.asSharedFlow()),
        Pair(MessageType.BAN, banUserFlowMut.asSharedFlow())
    )
}

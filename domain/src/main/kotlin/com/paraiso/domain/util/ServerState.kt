package com.paraiso.domain.util

import com.paraiso.domain.auth.User
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.util.Constants.EMPTY
import com.paraiso.domain.util.Constants.SYSTEM
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import java.util.UUID

object ServerState {
    val userList: MutableMap<String, User> = mutableMapOf()
    val banList: MutableSet<String> = mutableSetOf()

    val basePost = Post(
        id = UUID.randomUUID().toString(),
        userId = SYSTEM,
        title = "NBA",
        content = EMPTY,
        media = EMPTY,
        upVoted = emptySet(),
        downVoted = emptySet(),
        parentId = SYSTEM,
        status = PostStatus.ACTIVE,
        data = EMPTY,
        subPosts = emptySet(),
        createdOn = Clock.System.now(),
        updatedOn = Clock.System.now(),
    )
    val posts: MutableMap<String, Post> = mutableMapOf()

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

package com.paraiso.domain.util

import com.paraiso.domain.admin.PostReport
import com.paraiso.domain.admin.UserReport
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.posts.Post
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserChat
import com.paraiso.domain.users.UserResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

object ServerState {
    val userList: ConcurrentHashMap<String, User> = ConcurrentHashMap()
    val userChatList: ConcurrentHashMap<String, UserChat> = ConcurrentHashMap()
    val banList: MutableSet<String> = mutableSetOf()
    val userReports: ConcurrentHashMap<String, UserReport> = ConcurrentHashMap()
    val postReports: ConcurrentHashMap<String, PostReport> = ConcurrentHashMap()

    val posts: ConcurrentHashMap<String, Post> = ConcurrentHashMap()
    val routes: ConcurrentHashMap<String, RouteDetails> = ConcurrentHashMap()

    val messageFlowMut = MutableSharedFlow<Message>(replay = 0)
    val voteFlowMut = MutableSharedFlow<Vote>(replay = 0)
    val followFlowMut = MutableSharedFlow<Follow>(replay = 0)
    val deleteFlowMut = MutableSharedFlow<Delete>(replay = 0)
    val userUpdateFlowMut = MutableSharedFlow<UserResponse>(replay = 0)
    val banUserFlowMut = MutableSharedFlow<Ban>(replay = 0)
    val tagUserFlowMut = MutableSharedFlow<Tag>(replay = 0)
    val reportUserFlowMut = MutableSharedFlow<Report>(replay = 0)
    val reportPostFlowMut = MutableSharedFlow<Report>(replay = 0)

    val flowList = listOf( // convert to immutable for send to client
        Pair(MessageType.MSG, messageFlowMut.asSharedFlow()),
        Pair(MessageType.VOTE, voteFlowMut.asSharedFlow()),
        Pair(MessageType.FOLLOW, followFlowMut.asSharedFlow()),
        Pair(MessageType.DELETE, deleteFlowMut.asSharedFlow()),
        Pair(MessageType.USER_UPDATE, userUpdateFlowMut.asSharedFlow()),
        Pair(MessageType.BAN, banUserFlowMut.asSharedFlow()),
        Pair(MessageType.TAG, tagUserFlowMut.asSharedFlow()),
        Pair(MessageType.REPORT_USER, reportUserFlowMut.asSharedFlow()),
        Pair(MessageType.REPORT_POST, reportPostFlowMut.asSharedFlow())
    )
}

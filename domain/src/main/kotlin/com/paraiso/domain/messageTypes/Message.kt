package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    val userId: String,
    val userReceiveId: String,
    val title: String,
    val content: String,
    val type: PostType,
    val media: String,
    val replyId: String,
    val rootId: String,
    val editId: String,
    val route: SiteRoute
)

@Serializable
data class DirectMessage(
    val id: String? = null,
    val userId: String,
    val userReceiveId: String,
    val content: String,
    val media: String
)

fun Message.toNewPost() = Post(
    id = id ?: UNKNOWN,
    userId = userId,
    title = title,
    content = content,
    type = type,
    media = media,
    votes = mapOf(userId to true),
    parentId = replyId,
    rootId = rootId,
    status = PostStatus.ACTIVE,
    data = "",
    subPosts = emptySet(),
    route = route,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

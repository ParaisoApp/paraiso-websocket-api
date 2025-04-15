package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
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
    val editId: String
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
    upvoted = mapOf(userId to true),
    downvoted = emptyMap(),
    parentId = replyId,
    status = PostStatus.ACTIVE,
    data = "",
    subPosts = emptySet(),
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now(),
)

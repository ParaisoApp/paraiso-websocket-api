package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    val userId: String,
    val userReceiveId: String,
    val title: String,
    val content: String,
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
    id = id,
    userId = userId,
    title = title,
    content = content,
    media = media,
    upVoted = setOf(userId),
    downVoted = emptySet(),
    parentId = replyId,
    status = PostStatus.ACTIVE,
    data = "",
    subPosts = emptySet(),
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now(),
)

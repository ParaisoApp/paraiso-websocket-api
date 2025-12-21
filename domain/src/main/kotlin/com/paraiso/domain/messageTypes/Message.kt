package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.RecordSerializer
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    val userId: String?,
    @Serializable(with = RecordSerializer::class)
    val userReceiveIds: Set<String>,
    val title: String?,
    val content: String?,
    val type: PostType,
    val media: String?,
    val data: String?,
    val replyId: String?,
    val rootId: String?,
    val editId: String?,
    val route: String
)

fun Message.toNewPost() =
    Clock.System.now().let { now ->
        Post(
            id = id,
            userId = userId,
            title = title,
            content = content,
            type = type,
            media = media,
            score = 1,
            parentId = replyId,
            rootId = rootId,
            status = PostStatus.ACTIVE,
            data = data,
            count = 0,
            route = route,
            createdOn = now,
            updatedOn = now
        )
    }

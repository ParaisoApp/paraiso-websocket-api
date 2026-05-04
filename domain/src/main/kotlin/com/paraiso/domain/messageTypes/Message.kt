package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.ActiveStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.users.UserRole
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
    val route: String,
    val userRole: UserRole
): ServerEvent

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
            status = ActiveStatus.ACTIVE,
            data = data,
            count = 0,
            route = route,
            userVote = null,
            createdOn = now,
            updatedOn = now
        )
    }

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class UserChat(
    val id: String,
    val users: Set<User>,
    val dms: Set<DirectMessage>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class UserChatReturn(
    val id: String,
    val users: Map<String, UserResponse>,
    val dms: Map<String, DirectMessage>,
    val createdOn: Instant,
    val updatedOn: Instant
)

fun UserChat.toReturn() =
    UserChatReturn(
        id = id,
        users = users.map { it.buildUserResponse() }.associateBy { it.id },
        dms = dms.associateBy { it.id ?: UNKNOWN },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserChatReturn.toUserChat() =
    UserChat(
        id = id,
        users = users.values.map { it.toUser() }.toSet(),
        dms = dms.values.toSet(),
        createdOn = createdOn,
        updatedOn = updatedOn
    )

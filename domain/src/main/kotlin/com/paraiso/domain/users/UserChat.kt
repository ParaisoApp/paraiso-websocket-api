package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserChat(
    @SerialName(ID) val id: String,
    val userIds: Set<String>,
    val dms: Set<DirectMessage>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class UserChatReturn(
    val id: String,
    val userIds: Map<String, Boolean>,
    val dms: Map<String, DirectMessage>,
    val createdOn: Instant,
    val updatedOn: Instant
)

fun UserChat.toReturn() =
    UserChatReturn(
        id = id,
        userIds = userIds.associateWith { true },
        dms = dms.associateBy { it.id ?: UNKNOWN },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserChatReturn.toUserChat() =
    UserChat(
        id = id,
        userIds = userIds.keys,
        dms = dms.values.toSet(),
        createdOn = createdOn,
        updatedOn = updatedOn
    )

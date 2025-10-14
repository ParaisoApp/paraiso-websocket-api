package com.paraiso.domain.userchats

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserChat(
    @SerialName(ID) val id: String,
    val userIds: Set<String>,
    val recentDm: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant
)

@Serializable
data class UserChatResponse(
    val id: String,
    val userIds: Map<String, Boolean>,
    val dms: List<DirectMessageResponse>,
    val createdOn: Instant,
    val updatedOn: Instant
)

fun UserChat.toResponse(dms: List<DirectMessageResponse>) =
    UserChatResponse(
        id = id,
        userIds = userIds.associateWith { true },
        dms = dms,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

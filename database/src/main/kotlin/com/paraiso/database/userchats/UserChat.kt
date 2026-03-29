package com.paraiso.database.userchats

import com.paraiso.domain.userchats.DirectMessage
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.userchats.UserChat as UserChatDomain
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

fun UserChatDomain.toEntity() =
    UserChat(
        id = id,
        userIds = userIds.keys,
        recentDm = null,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserChat.toDomain() =
    UserChatDomain(
        id = id,
        userIds = userIds.associateWith { true },
        dms = emptyList(),
        createdOn = createdOn,
        updatedOn = updatedOn
    )

package com.paraiso.database.userchats

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.userchats.DirectMessage as DirectMessageDomain
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DirectMessage(
    @SerialName(ID) val id: String? = null,
    val chatId: String,
    val userId: String?,
    val userReceiveId: String,
    val content: String?,
    val media: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?
)

fun DirectMessageDomain.toEntity() = DirectMessage(
    id = id,
    chatId = chatId,
    userId = userId,
    userReceiveId = userReceiveId,
    content = content,
    media = media,
    createdOn = createdOn ?: Clock.System.now()
)

fun DirectMessage.toDomain() = DirectMessageDomain(
    id = id,
    chatId = chatId,
    userId = userId,
    userReceiveId = userReceiveId,
    content = content,
    media = media,
    createdOn = createdOn
)

package com.paraiso.domain.userchats

import com.paraiso.domain.util.Constants.ID
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

@Serializable
data class DirectMessageResponse(
    val id: String? = null,
    val chatId: String,
    val userId: String?,
    val userReceiveId: String,
    val content: String?,
    val media: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?
)

fun DirectMessageResponse.toDomain() = DirectMessage(
    id = id,
    chatId = chatId,
    userId = userId,
    userReceiveId = userReceiveId,
    content = content,
    media = media,
    createdOn = createdOn ?: Clock.System.now()
)

fun DirectMessage.toResponse() = DirectMessageResponse(
    id = id,
    chatId = chatId,
    userId = userId,
    userReceiveId = userReceiveId,
    content = content,
    media = media,
    createdOn = createdOn
)

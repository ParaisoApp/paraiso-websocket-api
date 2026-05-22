package com.paraiso.domain.userchats

import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DirectMessage(
    val id: String? = null,
    val chatId: String,
    val userId: String?,
    val userReceiveId: String,
    val content: String?,
    val media: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?
)

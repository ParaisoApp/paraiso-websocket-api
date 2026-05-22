package com.paraiso.domain.userchats

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserChat(
    val id: String,
    val userIds: Map<String, Boolean>,
    val dms: List<DirectMessage>,
    val createdOn: Instant,
    val updatedOn: Instant
)

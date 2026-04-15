package com.paraiso.domain.follows

import com.paraiso.domain.messageTypes.ServerEvent
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    val id: String? = null,
    val followerId: String,
    val followeeId: String,
    val following: Boolean,
    val createdOn: Instant? = Clock.System.now()
)

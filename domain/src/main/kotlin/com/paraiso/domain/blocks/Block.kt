package com.paraiso.domain.blocks

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Block(
    val id: String? = null,
    val blockerId: String,
    val blockeeId: String,
    val blocking: Boolean,
    val createdOn: Instant? = Clock.System.now()
)

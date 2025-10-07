package com.paraiso.domain.blocks

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Block(
    @SerialName(Constants.ID) val id: String,
    val blockerId: String,
    val blockeeId: String,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?
)

@Serializable
data class BlockResponse(
    val id: String? = null,
    val blockerId: String,
    val blockeeId: String,
    val blocking: Boolean,
    val createdOn: Instant? = Clock.System.now()
)
fun BlockResponse.toDomain() = Block(
    id = id ?: "$blockerId-$blockeeId",
    blockerId = blockerId,
    blockeeId = blockeeId,
    createdOn = createdOn ?: Clock.System.now()
)

fun Block.toResponse() = BlockResponse(
    id = id,
    blockerId = blockerId,
    blockeeId = blockeeId,
    blocking = true,
    createdOn = createdOn ?: Clock.System.now()
)

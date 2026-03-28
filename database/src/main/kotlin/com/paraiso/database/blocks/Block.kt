package com.paraiso.database.blocks

import com.paraiso.domain.blocks.Block as BlockDomain
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

fun BlockDomain.toEntity() = Block(
    id = id ?: "$blockerId-$blockeeId",
    blockerId = blockerId,
    blockeeId = blockeeId,
    createdOn = createdOn ?: Clock.System.now()
)

fun Block.toDomain() = BlockDomain(
    id = id,
    blockerId = blockerId,
    blockeeId = blockeeId,
    blocking = true,
    createdOn = createdOn ?: Clock.System.now()
)
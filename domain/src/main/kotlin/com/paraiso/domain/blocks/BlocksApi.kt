package com.paraiso.domain.blocks

class BlocksApi(
    private val blocksDB: BlocksDB
) {

    suspend fun block(block: BlockResponse) =
        if (block.blocking) {
            blocksDB.delete(block.blockerId, block.blockeeId)
        } else {
            blocksDB.save(listOf(block.toDomain()))
        }
    suspend fun findIn(blockerId: String, blockeeIds: List<String>) =
        blocksDB.findIn(blockerId, blockeeIds).map { it.toResponse() }
}

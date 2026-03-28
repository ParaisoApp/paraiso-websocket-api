package com.paraiso.domain.blocks

class BlocksApi(
    private val blocksDB: BlocksDB
) {

    suspend fun block(block: Block) =
        if (block.blocking) {
            blocksDB.delete(block.blockerId, block.blockeeId)
        } else {
            blocksDB.save(listOf(block))
        }
    suspend fun findIn(blockerId: String, blockeeIds: List<String>) =
        blocksDB.findIn(blockerId, blockeeIds)
}

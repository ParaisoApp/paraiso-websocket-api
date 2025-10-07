package com.paraiso.domain.blocks

interface BlocksDB {
    suspend fun findIn(blockerId: String, blockeeIds: List<String>): List<Block>
    suspend fun save(blocks: List<Block>): Int
    suspend fun delete(blockerId: String, blockeeIds: String): Long
}

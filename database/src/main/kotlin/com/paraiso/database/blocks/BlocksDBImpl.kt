package com.paraiso.database.blocks

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.blocks.Block
import com.paraiso.domain.blocks.BlocksDB
import com.paraiso.domain.follows.Follow
import com.paraiso.domain.follows.FollowsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class BlocksDBImpl(database: MongoDatabase) : BlocksDB {

    private val collection = database.getCollection("blocks", Block::class.java)

    override suspend fun findIn(blockerId: String, blockeeIds: List<String>) =
        if(blockeeIds.size == 1){
            collection.find(
                and(
                    eq(Block::blockerId.name, blockerId),
                    eq(Block::blockeeId.name, blockeeIds.firstOrNull())
                )
            ).toList()
        }else{
            collection.find(
                and(
                    eq(Block::blockerId.name, blockerId),
                    `in`(Block::blockeeId.name, blockeeIds)
                )
            ).toList()
        }

    override suspend fun save(blocks: List<Block>): Int {
        val bulkOps = blocks.map { block ->
            ReplaceOneModel(
                eq(ID, block.id),
                block,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun delete(blockerId: String, blockeeId: String) =
        collection.deleteOne(
            and(
                eq(Block::blockerId.name, blockerId),
                eq(Block::blockeeId.name, blockeeId)
            )
        ).deletedCount
}

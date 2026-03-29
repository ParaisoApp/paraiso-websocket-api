package com.paraiso.database.userchats

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.userchats.DirectMessage as DirectMessageDomain
import com.paraiso.domain.userchats.DirectMessagesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class DirectMessagesDBImpl(database: MongoDatabase) : DirectMessagesDB {

    private val collection = database.getCollection("directMessages", DirectMessage::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            if (ids.size == 1) {
                collection.find(
                    and(
                        eq(ID, ids.firstOrNull())
                    )
                ).map { it.toDomain() }.toList()
            } else {
                collection.find(
                    and(
                        `in`(ID, ids)
                    )
                ).map { it.toDomain() }.toList()
            }
        }
    override suspend fun findByChatId(chatId: String, userId: String) =
        withContext(Dispatchers.IO) {
            collection.find(
                and(
                    eq(DirectMessage::chatId.name, chatId),
                    or(
                        eq(DirectMessage::userId.name, userId),
                        eq(DirectMessage::userReceiveId.name, userId),
                    )
                )
            ).map { it.toDomain() }.toList()
        }

    override suspend fun save(dms: List<DirectMessageDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = dms.map { dm ->
                val entity = dm.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}

package com.paraiso.database.userchats

import com.mongodb.client.model.Filters.all
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.userchats.UserChat
import com.paraiso.domain.userchats.UserChatsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class UserChatsDBImpl(database: MongoDatabase) : UserChatsDB {
    private val collection = database.getCollection("userChats", UserChat::class.java)

    override suspend fun findByUserIds(userId: String, otherUserId: String) =
        withContext(Dispatchers.IO) {
            collection.find(all(UserChat::userIds.name, listOf(userId, otherUserId))).limit(1).firstOrNull()
        }
    override suspend fun findByUserId(userId: String) =
        withContext(Dispatchers.IO) {
            collection.find(eq(UserChat::userIds.name, userId)).toList()
        }

    override suspend fun save(chats: List<UserChat>) =
        withContext(Dispatchers.IO) {
            val bulkOps = chats.map { chat ->
                ReplaceOneModel(
                    eq(ID, chat.id),
                    chat,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun setMostRecentDm(
        dmId: String,
        chatId: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, chatId),
                combine(
                    set(UserChat::recentDm.name, dmId),
                    set(UserChat::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
}

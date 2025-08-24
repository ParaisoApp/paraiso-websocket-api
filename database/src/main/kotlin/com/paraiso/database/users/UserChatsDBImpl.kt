package com.paraiso.database.users

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.users.UserChat
import com.paraiso.domain.users.UserChatsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class UserChatsDBImpl(database: MongoDatabase) : UserChatsDB {
    private val collection = database.getCollection("userChats", UserChat::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun save(chats: List<UserChat>): Int {
        val bulkOps = chats.map { chat ->
            ReplaceOneModel(
                Filters.eq(ID, chat.id),
                chat,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun addToUserChat(
        chatId: String,
        dm: DirectMessage
    ) =
        collection.updateOne(
            Filters.eq(ID, chatId),
            combine(
                addToSet(UserChat::dms.name, dm),
                set(UserChat::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount
}

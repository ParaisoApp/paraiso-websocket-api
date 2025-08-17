package com.paraiso.database.users

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.users.UserChat
import com.paraiso.domain.users.UserChatsDBAdapter
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class UserChatsDBAdapterImpl(database: MongoDatabase) : UserChatsDBAdapter {
    private val collection = database.getCollection("userChats", UserChat::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    suspend fun save(chats: List<UserChat>) =
        collection.insertMany(chats)

    suspend fun setUserChat(
        chatId: String,
        dm: DirectMessage
    ) =
        collection.updateOne(
            Filters.eq(ID, chatId),
            combine(
                addToSet(UserChat::dms.name, dm),
                set(UserChat::updatedOn.name, Clock.System.now())
            )
        )
}

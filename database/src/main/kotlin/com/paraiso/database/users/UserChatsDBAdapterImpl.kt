package com.paraiso.database.users

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import com.paraiso.domain.users.ChatRef
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserChat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class UserChatsDBAdapterImpl(database: MongoDatabase): RoutesDBAdapter {
    private val collection = database.getCollection("userChats", UserChat::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(UserChat::id.name, id)).firstOrNull()

    suspend fun save(routes: List<UserChat>) =
        collection.insertMany(routes)

    suspend fun setUserChat(
        chatId: String,
        dm: DirectMessage,
    ) =
        collection.updateOne(
            Filters.eq(UserChat::id.name, chatId),
            Updates.combine(
                Updates.addToSet(UserChat::dms.name, dm),
                Updates.set(UserChat::updatedOn.name, Clock.System.now())
            )
        )
}
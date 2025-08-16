package com.paraiso.database.users

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import com.paraiso.domain.users.UserChat
import kotlinx.coroutines.flow.firstOrNull

class UserChatsDBAdapterImpl(database: MongoDatabase): RoutesDBAdapter {
    private val collection = database.getCollection("userChats", UserChat::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(UserChat::id.name, id)).firstOrNull()

    suspend fun save(routes: List<UserChat>) =
        collection.insertMany(routes)
}
package com.paraiso.database.users

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.users.UserSession
import com.paraiso.domain.users.UserSessionsDBAdapter
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

class UserSessionsDBAdapterImpl(database: MongoDatabase) : UserSessionsDBAdapter {
    private val collection = database.getCollection("userSessions", UserSession::class.java)
    override suspend fun get() =
        collection.find().toList()

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()

    override suspend fun findByUserId(id: String) =
        collection.find(eq(UserSession::userId.name, id)).firstOrNull()

    override suspend fun findByUserIdsIn(ids: List<String>) =
        collection.find(`in`(UserSession::userId.name, ids)).toList()

    override suspend fun save(userSessions: List<UserSession>): Int {
        val bulkOps = userSessions.map { userSession ->
            ReplaceOneModel(
                eq(ID, userSession.id),
                userSession,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun setConnectedByUserId(
        userId: String,
        status: UserStatus
    ) =
        collection.updateOne(
            eq(UserSession::userId.name, userId),
            combine(
                set(UserSession::status.name, status),
                set(UserSession::lastSeen.name, Clock.System.now())
            )
        ).modifiedCount
}

package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.UserReport
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import com.paraiso.domain.users.ChatRef
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserChat
import com.paraiso.domain.users.UserChatsDBAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class UserReportsDBAdapterImpl(database: MongoDatabase): UserChatsDBAdapter {
    private val collection = database.getCollection("userReports", UserReport::class.java)

    fun get() =
        collection.find()

    suspend fun save(userReports: List<UserReport>) =
        collection.insertMany(userReports)

    suspend fun addUserReport(
        userId: String,
        reportingUserId: String,
    ) =
        collection.updateOne(
            Filters.eq(UserReport::userId.name, userId),
            Updates.combine(
                Updates.addToSet(UserReport::reportedBy.name, reportingUserId),
                Updates.set(UserChat::updatedOn.name, Clock.System.now())
            )
        )
}
package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.PostReport
import com.paraiso.domain.admin.PostReportsDBAdapter
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

class PostReportsDBAdapterImpl(database: MongoDatabase): PostReportsDBAdapter {
    private val collection = database.getCollection("postReports", PostReport::class.java)

    fun get() =
        collection.find()

    suspend fun save(postReports: List<PostReport>) =
        collection.insertMany(postReports)

    suspend fun addUserReport(
        userId: String,
        reportingUserId: String,
    ) =
        collection.updateOne(
            Filters.eq(PostReport::postId.name, userId),
            combine(
                addToSet(PostReport::reportedBy.name, reportingUserId),
                set(PostReport::updatedOn.name, Clock.System.now())
            )
        )
}
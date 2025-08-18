package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.PostReport
import com.paraiso.domain.admin.PostReportsDBAdapter
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

class PostReportsDBAdapterImpl(database: MongoDatabase) : PostReportsDBAdapter {
    private val collection = database.getCollection("postReports", PostReport::class.java)

    override suspend fun getAll() =
        collection.find().sort(ascending(PostReport::updatedOn.name)).toList()

    override suspend fun save(postReports: List<PostReport>) =
        collection.insertMany(postReports).insertedIds.map { it.toString() }

    override suspend fun addPostReport(
        postId: String,
        reportingUserId: String
    ) =
        collection.updateOne(
            Filters.eq(ID, postId),
            combine(
                addToSet(PostReport::reportedBy.name, reportingUserId),
                set(PostReport::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount
}

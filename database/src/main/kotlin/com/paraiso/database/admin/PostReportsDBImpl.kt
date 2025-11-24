package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.PostReport
import com.paraiso.domain.admin.PostReportsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class PostReportsDBImpl(database: MongoDatabase) : PostReportsDB {
    private val collection = database.getCollection("postReports", PostReport::class.java)

    override suspend fun getAll() =
        withContext(Dispatchers.IO) {
            collection.find().sort(ascending(PostReport::updatedOn.name)).toList()
        }

    override suspend fun save(postReports: List<PostReport>) =
        withContext(Dispatchers.IO) {
            val bulkOps = postReports.map { report ->
                ReplaceOneModel(
                    Filters.eq(ID, report.id),
                    report,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun addPostReport(
        postId: String,
        reportingUserId: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                Filters.eq(ID, postId),
                combine(
                    addToSet(PostReport::reportedBy.name, reportingUserId),
                    set(PostReport::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
}

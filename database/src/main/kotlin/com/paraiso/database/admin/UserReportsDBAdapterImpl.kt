package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.UserReport
import com.paraiso.domain.admin.UserReportsDBAdapter
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

class UserReportsDBAdapterImpl(database: MongoDatabase) : UserReportsDBAdapter {
    private val collection = database.getCollection("userReports", UserReport::class.java)

    override suspend fun getAll() =
        collection.find().sort(ascending(UserReport::updatedOn.name)).toList()

    override suspend fun save(userReports: List<UserReport>): Int {
        val bulkOps = userReports.map { report ->
            ReplaceOneModel(
                Filters.eq(ID, report.id),
                report,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun addUserReport(
        userId: String,
        reportingUserId: String
    ) =
        collection.updateOne(
            Filters.eq(ID, userId),
            combine(
                addToSet(UserReport::reportedBy.name, reportingUserId),
                set(UserReport::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount
}

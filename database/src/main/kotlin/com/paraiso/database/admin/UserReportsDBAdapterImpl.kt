package com.paraiso.database.admin

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.admin.UserReport
import com.paraiso.domain.admin.UserReportsDBAdapter
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import kotlinx.datetime.Clock

class UserReportsDBAdapterImpl(database: MongoDatabase) : UserReportsDBAdapter {
    private val collection = database.getCollection("userReports", UserReport::class.java)

    fun get() =
        collection.find()

    suspend fun save(userReports: List<UserReport>) =
        collection.insertMany(userReports)

    suspend fun addUserReport(
        userId: String,
        reportingUserId: String
    ) =
        collection.updateOne(
            Filters.eq(ID, userId),
            combine(
                addToSet(UserReport::reportedBy.name, reportingUserId),
                set(UserReport::updatedOn.name, Clock.System.now())
            )
        )
}

package com.paraiso.domain.admin

import com.paraiso.domain.messageTypes.Report
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AdminApi(
    private val postReportsDB: PostReportsDB,
    private val userReportsDB: UserReportsDB
) {
    suspend fun getUserReports() =
        userReportsDB.getAll().map { userReport ->
            userReport.toResponse()
        }

    suspend fun getPostReports() =
        postReportsDB.getAll().map { postReport ->
            postReport.toResponse()
        }

    suspend fun reportUser(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            val modifiedCount = userReportsDB.addUserReport(report.id, sessionUserId)
            if (modifiedCount == 0L) {
                userReportsDB.save(
                    listOf(
                        UserReport(
                            id = report.id,
                            reportedBy = setOf(sessionUserId),
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
            }
        }
    }

    suspend fun reportPost(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            val modifiedCount = postReportsDB.addPostReport(report.id, sessionUserId)
            if (modifiedCount == 0L) {
                postReportsDB.save(
                    listOf(
                        PostReport(
                            id = report.id,
                            reportedBy = setOf(sessionUserId),
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
            }
        }
    }
}

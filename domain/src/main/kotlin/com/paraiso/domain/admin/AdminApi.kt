package com.paraiso.domain.admin

import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UsersApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AdminApi(
    private val postReportsDB: PostReportsDB,
    private val userReportsDB: UserReportsDB,
    private val usersApi: UsersApi
) {
    suspend fun getUserReports() =
        userReportsDB.getAll()

    suspend fun getPostReports() =
        postReportsDB.getAll()

    suspend fun reportUser(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            val modifiedCount = userReportsDB.addUserReport(report.id, sessionUserId)
            if (modifiedCount == 0L) {
                userReportsDB.save(
                    listOf(
                        UserReport(
                            userId = report.id,
                            reportedBy = mapOf(sessionUserId to true),
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
                            postId = report.id,
                            reportedBy = mapOf(sessionUserId to true),
                            createdOn = now,
                            updatedOn = now
                        )
                    )
                )
            }
        }
    }
}

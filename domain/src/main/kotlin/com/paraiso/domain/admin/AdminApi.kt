package com.paraiso.domain.admin

import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.posts.toResponse
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.buildUserResponse
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AdminApi(
    private val postReportsDBAdapter: PostReportsDBAdapter,
    private val userReportsDBAdapter: UserReportsDBAdapter
) {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    suspend fun getUserReports() =
        userReportsDBAdapter.getAll().mapNotNull { userReport ->
            ServerState.userList[userReport.id]?.let { user ->
                userReport.toResponse(user.buildUserResponse())
            } ?: run { null }
        }

    suspend fun getPostReports() =
        postReportsDBAdapter.getAll().mapNotNull { postReport ->
            ServerState.posts[postReport.id]?.let { post ->
                postReport.toResponse(post.toResponse())
            } ?: run { null }
        }

    suspend fun reportUser(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            val modifiedCount = userReportsDBAdapter.addUserReport(report.id, sessionUserId)
            if(modifiedCount == 0L){
                userReportsDBAdapter.save(
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
            val modifiedCount = postReportsDBAdapter.addPostReport(report.id, sessionUserId)
            if(modifiedCount == 0L) {
                postReportsDBAdapter.save(
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

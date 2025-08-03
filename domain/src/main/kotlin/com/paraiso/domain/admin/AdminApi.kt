package com.paraiso.domain.admin

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.posts.toPostReturn
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.buildUserResponse
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AdminApi {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    fun getUserReports() =
        ServerState.userReports.values.sortedBy { it.updatedOn }

    fun getPostReports() =
        ServerState.postReports.values.sortedBy { it.updatedOn }

    suspend fun reportUser(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            ServerState.userReports[report.id]?.let { userReport ->
                userReport.reportedBy.toMutableSet().let { mutableReports ->
                    mutableReports.add(sessionUserId)
                    ServerState.userReports[report.id] = userReport.copy(
                        reportedBy = mutableReports,
                        updatedOn = now
                    )
                }
            } ?: run {
                ServerState.userList[report.id]?.let { user ->
                    ServerState.userReports[report.id] = UserReport(
                        user = user.buildUserResponse(),
                        reportedBy = setOf(sessionUserId),
                        createdOn = now,
                        updatedOn = now
                    )
                }
            }
        }
        ServerState.userList.values.filter {
            it.roles == UserRole.ADMIN || it.roles == UserRole.MOD
        }.forEach { user ->
            user.userReports.toMutableMap().let { mutableReports ->
                mutableReports[report.id] = false
                ServerState.userList[user.id] = user.copy(
                    userReports = mutableReports,
                    updatedOn = now
                )
            }
        }
    }

    suspend fun reportPost(sessionUserId: String, report: Report) = coroutineScope {
        val now = Clock.System.now()
        launch {
            ServerState.postReports[report.id]?.let{ postReport ->
                postReport.reportedBy.toMutableSet().let { mutableReports ->
                    mutableReports.add(sessionUserId)
                    ServerState.postReports[report.id] = postReport.copy(
                        reportedBy = mutableReports,
                        updatedOn = now
                    )
                }
            } ?: run {
                ServerState.posts[report.id]?.let { post ->
                    ServerState.postReports[report.id] = PostReport(
                        post = post.toPostReturn(),
                        reportedBy = setOf(sessionUserId),
                        createdOn = now,
                        updatedOn = now
                    )
                }
            }
        }

        ServerState.userList.values.filter {
            it.roles == UserRole.ADMIN || it.roles == UserRole.MOD
        }.forEach { user ->
            user.postReports.toMutableMap().let { mutableReports ->
                mutableReports[report.id] = false
                ServerState.userList[user.id] = user.copy(
                    postReports = mutableReports,
                    updatedOn = Clock.System.now()
                )
            }
        }

        }

    fun banUser(ban: Ban) =
        ServerState.banList.add(ban.userId)

}

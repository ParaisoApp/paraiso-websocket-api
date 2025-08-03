package com.paraiso.domain.admin

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.posts.toPostReturn
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.buildUserResponse
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock

class AdminApi {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    fun getUserReports() =
        ServerState.userReports.map {
            ServerState.userList[it.key]?.let{user ->
                user.buildUserResponse() to it.value
            } ?: run { null }
        }.filterNotNull().toMap()

    fun getPostReports() =
        ServerState.postReports.map {
            ServerState.posts[it.key]?.let{post ->
                post.toPostReturn() to it.value
            } ?: run { null }
        }.filterNotNull().toMap()

    fun reportUser(sessionUserId: String, report: Report) {
        ServerState.userReports[report.id]?.toMutableSet()?.let{ mutableReports ->
            mutableReports.add(sessionUserId)
            ServerState.userReports[report.id] = mutableReports
        } ?: run {
            ServerState.userReports[report.id] = setOf(sessionUserId)
        }
        ServerState.userList.values.filter {
            it.roles == UserRole.ADMIN || it.roles == UserRole.MOD
        }.forEach { user ->
            user.userReports.toMutableMap().let { mutableReports ->
                mutableReports[report.id] = false
                ServerState.userList[user.id] = user.copy(
                    userReports = mutableReports,
                    updatedOn = Clock.System.now()
                )
            }
        }
    }

    fun reportPost(sessionUserId: String, report: Report)
        {
            ServerState.postReports[report.id]?.toMutableSet()?.let{ mutableReports ->
                mutableReports.add(sessionUserId)
                ServerState.postReports[report.id] = mutableReports
            } ?: run {
                ServerState.postReports[report.id] = setOf(sessionUserId)
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

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.FilterTypes

class UserSessionsApi(
    private val usersDBAdapter: UsersDBAdapter,
    private val userSessionsDBAdapter: UserSessionsDBAdapter
) {

    companion object {
        const val RETRIEVE_LIM = 5
    }

    suspend fun getUserList(filters: FilterTypes, userId: String) =
        userSessionsDBAdapter.get().map { it.userId }.let{ activeUserIds ->
            usersDBAdapter.getFollowingById(userId).let { followingList ->
                usersDBAdapter.getUserList(activeUserIds, filters, followingList.map { it.id })
            }.associate { user -> user.id to user.buildUserResponse() }
        }
    suspend fun getByUserId(userId: String) =
        userSessionsDBAdapter.findByUserId(userId)?.toResponse()

    suspend fun save(userSessions: List<UserSession>) =
        userSessionsDBAdapter.save(userSessions)
    suspend fun setConnected(userId: String, status: UserStatus) =
        userSessionsDBAdapter.setConnectedByUserId(userId, status)
}

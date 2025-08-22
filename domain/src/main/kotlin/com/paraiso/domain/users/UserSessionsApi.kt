package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.FilterTypes

class UserSessionsApi(
    private val usersDBAdapter: UsersDBAdapter,
    private val eventService: EventService
) {

    suspend fun getUserList(filters: FilterTypes, userId: String) =
        eventService.getAllActiveUsers().map { it.userId }.let{ activeUserIds ->
            usersDBAdapter.getFollowingById(userId).let { followingList ->
                usersDBAdapter.getUserList(activeUserIds, filters, followingList.map { it.id })
            }.associate { user -> user.id to user.buildUserResponse() }
        }
    fun getByUserId(userId: String) =
        eventService.getUserSession(userId)?.toResponse(UserStatus.CONNECTED)

}

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.FilterTypes

class UserSessionsApi(
    private val usersDB: UsersDB,
    private val eventService: EventService
) {

    suspend fun getUserList(filters: FilterTypes, userId: String) =
        eventService.getAllActiveUsers().map { it.userId }.let{ activeUserIds ->
            usersDB.getFollowingById(userId).let { followingList ->
                usersDB.getUserList(activeUserIds, filters, followingList.map { it.id })
            }.associate { user -> user.id to user.buildUserResponse() }
        }
    suspend fun getByUserId(userId: String) =
        eventService.getUserSession(userId)?.toResponse(UserStatus.CONNECTED)

}

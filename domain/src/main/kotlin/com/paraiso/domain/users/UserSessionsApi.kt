package com.paraiso.domain.users

import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.messageTypes.FilterTypes

class UserSessionsApi(
    private val usersDB: UsersDB,
    private val eventService: EventService,
    private val followsApi: FollowsApi
) {

    private fun getStatus(session: UserSession?) =
        if(session == null){
            UserStatus.DISCONNECTED
        } else UserStatus.CONNECTED
    suspend fun getUserById(userId: String) =
        eventService.getUserSession(userId).let {session ->
            usersDB.findById(userId)?.toBasicResponse(getStatus(session))
        }

    suspend fun getUserByName(userName: String): UserResponse? =
        usersDB.findByName(userName)?.let{user ->
            eventService.getUserSession(user.id).let { session ->
                user.toBasicResponse(getStatus(session))
            }
        }

    suspend fun exists(search: String) =
        usersDB.existsByName(search)

    suspend fun getUserByPartial(search: String) =
        usersDB.findByPartial(search)
            .map {user ->
                eventService.getUserSession(user.id).let { session ->
                    user.toBasicResponse(getStatus(session))
                }
            }

    suspend fun getFollowingById(userId: String) =
        followsApi.getByFollowerId(userId).map { it.followeeId }
            .let { followeeIds ->
                val followees = usersDB.findByIdIn(followeeIds)
                followees.map {
                    eventService.getUserSession(it.id).let { session ->
                        it.toBasicResponse(getStatus(session))
                    }
                }
            }

    suspend fun getFollowersById(userId: String) =
        followsApi.getByFolloweeId(userId).map { it.followerId }
            .let { followerIds ->
                val followers = usersDB.findByIdIn(followerIds)
                followers.map {
                    eventService.getUserSession(it.id).let { session ->
                        it.toBasicResponse(getStatus(session))
                    }
                }
            }

    suspend fun getUserList(filters: FilterTypes, userId: String) =
        eventService.getAllActiveUsers().map { it.userId }.let{ activeUserIds ->
            val followees = followsApi.getByFollowerId(userId).map { it.followeeId }
            usersDB.getUserList(activeUserIds, filters, followees)
                .associate { user -> user.id to user.toBasicResponse(UserStatus.CONNECTED) }
        }

}

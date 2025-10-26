package com.paraiso.domain.users

import com.paraiso.domain.blocks.BlocksApi
import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.messageTypes.FilterTypes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class UserSessionsApi(
    private val usersDB: UsersDB,
    private val eventService: EventService,
    private val followsApi: FollowsApi,
    private val blocksApi: BlocksApi
) {

    private fun getStatus(session: UserSession?, hidden: Boolean) =
        if (session == null || hidden) {
            UserStatus.DISCONNECTED
        } else {
            UserStatus.CONNECTED
        }
    suspend fun getUserById(userId: String, curUserId: String?) = coroutineScope {
        eventService.getUserSession(userId).let { session ->
            val follows = async {
                if (curUserId != null) {
                    followsApi.findIn(curUserId, listOf(userId)).firstOrNull()
                } else {
                    null
                }
            }
            val blocks = async {
                if (curUserId != null) {
                    blocksApi.findIn(curUserId, listOf(userId)).firstOrNull()
                } else {
                    null
                }
            }
            usersDB.findById(userId)?.let {
                it.toBasicResponse(
                    getStatus(session, it.settings.hidden),
                    ViewerContext(
                        follows.await()?.following,
                        blocks.await()?.blocking,
                    )
                )
            }
        }
    }

    suspend fun getUserByName(userName: String, curUserId: String): UserResponse? = coroutineScope {
        usersDB.findByName(userName)?.let { user ->
            val follows = async { followsApi.findIn(curUserId, listOf(user.id)).firstOrNull() }
            val blocks = async { blocksApi.findIn(curUserId, listOf(user.id)).firstOrNull() }
            eventService.getUserSession(user.id).let { session ->
                user.toBasicResponse(
                    getStatus(session, user.settings.hidden),
                    ViewerContext(
                        follows.await()?.following,
                        blocks.await()?.blocking,
                    )
                )
            }
        }
    }

    suspend fun exists(search: String) =
        usersDB.existsByName(search)

    suspend fun getUserByPartial(search: String, curUserId: String) = coroutineScope {
        usersDB.findByPartial(search).let { users ->
            val userIds = users.map { it.id }
            val follows = async { followsApi.findIn(curUserId, userIds).associateBy { it.followeeId } }
            val blocks = async { blocksApi.findIn(curUserId, userIds).associateBy { it.blockeeId } }
            users.map { user ->
                eventService.getUserSession(user.id).let { session ->
                    user.toBasicResponse(
                        getStatus(session, user.settings.hidden),
                        ViewerContext(
                            follows.await()[user.id]?.following,
                            blocks.await()[user.id]?.blocking
                        )
                    )
                }
            }
        }
    }

    suspend fun getFollowingById(userId: String) = coroutineScope {
        followsApi.getByFollowerId(userId).map { it.followeeId }
            .let { followeeIds ->
                val followees = usersDB.findByIdIn(followeeIds)
                val blocks = async { blocksApi.findIn(userId, followeeIds).associateBy { it.blockeeId } }
                followees.associate { user ->
                    eventService.getUserSession(user.id).let { session ->
                        user.id to user.toBasicResponse(
                            getStatus(session, user.settings.hidden),
                            ViewerContext(
                                true,
                                    blocks.await()[user.id]?.blocking
                                )
                        )
                    }
                }
            }
    }

    suspend fun getFollowersById(userId: String) = coroutineScope {
        followsApi.getByFolloweeId(userId).map { it.followerId }
            .let { followerIds ->
                val followers = usersDB.findByIdIn(followerIds)
                val follows = async { followsApi.findIn(userId, followerIds).associateBy { it.followeeId } }
                val blocks = async { blocksApi.findIn(userId, followerIds).associateBy { it.blockeeId } }
                followers.associate { user ->
                    eventService.getUserSession(user.id).let { session ->
                        user.id to user.toBasicResponse(
                            getStatus(session, user.settings.hidden),
                            ViewerContext(
                                follows.await()[user.id]?.following,
                                blocks.await()[user.id]?.blocking
                            )
                        )
                    }
                }
            }
    }

    suspend fun getUserList(filters: FilterTypes, userId: String) = coroutineScope {
        eventService.getAllActiveUsers().map { it.userId }.let { activeUserIds ->
            val followees = followsApi.getByFollowerId(userId).map { it.followeeId }
            usersDB.getUserList(activeUserIds, filters, followees)
                .let { userList ->
                    val userIds = userList.map { it.id }
                    val follows = followsApi.findIn(userId, userIds).associateBy { it.followeeId }
                    val blocks = async { blocksApi.findIn(userId, userIds).associateBy { it.blockeeId } }
                    userList.associate {
                            user ->
                        user.id to user.toBasicResponse(
                            UserStatus.CONNECTED,
                            ViewerContext(
                                follows[user.id]?.following,
                                blocks.await()[user.id]?.blocking
                            )
                        )
                    }
                }
        }
    }
}

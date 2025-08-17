package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UsersApi {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    fun getUserById(userId: String) =
        ServerState.userList[userId]?.buildUserResponse()

    fun getUserByName(userName: String): UserResponse? =
        ServerState.userList.values.find { it.name == userName }?.buildUserResponse()

    fun exists(search: String) =
        ServerState.userList.values.any { it.name?.lowercase() == search.lowercase() }

    fun getUserByPartial(search: String) =
        ServerState.userList.values
            .filter { it.name?.lowercase()?.contains(search.lowercase()) == true }
            .take(PARTIAL_RETRIEVE_LIM)
            .map { it.buildUserResponse() }

    fun getUserList(filters: FilterTypes, userId: String) =
        ServerState.userList[userId]?.following?.let { followingList ->
            ServerState.userList.values
                .filter { user ->
                    user.status != UserStatus.DISCONNECTED &&
                        (
                            filters.userRoles.contains(user.roles) ||
                                (filters.userRoles.contains(UserRole.FOLLOWING) && followingList.contains(user.id))
                            )
                }.associate { user -> user.id to user.buildUserResponse() }
        }

    fun getFollowingById(userId: String) =
        ServerState.userList.values.filter {
            it.following.contains(userId)
        }.map { it.buildUserResponse() }

    fun getFollowersById(userId: String) =
        ServerState.userList.values.filter {
            it.followers.contains(userId)
        }.map { it.buildUserResponse() }

    fun saveUser(user: UserResponse) {
        ServerState.userList[user.id] = user.copy(
            updatedOn = Clock.System.now()
        ).toUser()
    }

    private fun addMention(user: User, messageId: String): String {
        user.replies.toMutableMap().let { mutableReplies ->
            mutableReplies[messageId] = false
            ServerState.userList[user.id] = user.copy(
                replies = mutableReplies,
                updatedOn = Clock.System.now()
            )
        }
        return user.id
    }

    suspend fun addMentions(userNames: Set<String>, userId: String?, messageId: String): Set<String> = coroutineScope {
        // update user post replies
        val userIds = userNames.map { userName ->
            async {
                ServerState.userList.values.find { it.name == userName }?.let { user ->
                    addMention(user, messageId)
                } ?: run { null }
            }
        }.awaitAll().filterNotNull().toMutableSet()
        // if (message.userId != message.userReceiveId) {
        if (userId != null) {
            ServerState.userList[userId]?.let { user ->
                userIds.add(addMention(user, messageId))
            }
        }
        userIds.toSet()
    }

    fun setSettings(userId: String, settings: UserSettings) =
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                settings = settings,
                updatedOn = Clock.System.now()
            )
        }

    suspend fun markNotifsRead(userId: String, userNotifs: UserNotifs) = coroutineScope {
        ServerState.userList[userId]?.let { user ->
            // grab chats and make mutable
            val userChats = async {
                user.chats.filter { userNotifs.userChatIds.contains(it.key) }
                    .toMutableMap().let { mutableChats ->
                        // find chat and set to viewed
                        mutableChats.map {
                            mutableChats[it.key] = it.value.copy(
                                viewed = true
                            )
                        }
                        mutableChats
                    }
            }
            // grab replies and make mutable
            val replies = async {
                user.replies.filter { userNotifs.replyIds.contains(it.key) }
                    .toMutableMap().let { mutableReplies ->
                        // find reply and set to viewed
                        mutableReplies.map {
                            mutableReplies[it.key] = true
                        }
                        mutableReplies
                    }
            }
            // update user
            ServerState.userList[userId] = user.copy(
                chats = userChats.await(),
                replies = replies.await(),
                updatedOn = Clock.System.now()
            )
        }
    }

    suspend fun markReportNotifsRead(userId: String, userReportNotifs: UserReportNotifs) = coroutineScope {
        ServerState.userList[userId]?.let { user ->
            // grab user reports and make mutable
            val userReports = async {
                user.userReports.filter { userReportNotifs.userIds.contains(it.key) }
                    .toMutableMap().let { mutableUserReports ->
                        // find user report and set to viewed
                        mutableUserReports.map {
                            mutableUserReports[it.key] = true
                        }
                        mutableUserReports
                    }
            }
            // grab post reports and make mutable
            val postReports = async {
                user.postReports.filter { userReportNotifs.postIds.contains(it.key) }
                    .toMutableMap().let { mutablePostReports ->
                        // find post report and set to viewed
                        mutablePostReports.map {
                            mutablePostReports[it.key] = true
                        }
                        mutablePostReports
                    }
            }

            // update user
            ServerState.userList[userId] = user.copy(
                userReports = userReports.await(),
                postReports = postReports.await(),
                updatedOn = Clock.System.now()
            )
        }
    }

    suspend fun follow(follow: Follow) = coroutineScope {
        val now = Clock.System.now()
        // add follower to followers list of followee user
        launch {
            ServerState.userList[follow.followeeId]?.let { followee ->
                followee.followers.toMutableSet().apply {
                    if (followee.followers.contains(follow.followerId)) {
                        remove(follow.followerId)
                    } else {
                        add(follow.followerId)
                    }
                }.let { updatedFollowers ->
                    ServerState.userList[followee.id] = followee.copy(
                        followers = updatedFollowers,
                        updatedOn = now
                    )
                }
            }
        }
        // add followee to following list of follower user
        ServerState.userList[follow.followerId]?.let { follower ->
            follower.following.toMutableSet().apply {
                if (follower.following.contains(follow.followeeId)) {
                    remove(follow.followeeId)
                } else {
                    add(follow.followeeId)
                }
            }.let { updatedFollowing ->
                ServerState.userList[follower.id] = follower.copy(
                    following = updatedFollowing,
                    updatedOn = now
                )
            }
        }
    }

    fun toggleBlockUser(userId: String, userBlockId: String) {
        ServerState.userList[userId]?.let { user ->
            user.blockList.toMutableSet().let { mutableBlockSet ->
                if (mutableBlockSet.contains(userBlockId)) {
                    mutableBlockSet.remove(userBlockId)
                } else {
                    mutableBlockSet.add(userBlockId)
                }
                ServerState.userList[userId] = user.copy(
                    blockList = mutableBlockSet,
                    updatedOn = Clock.System.now()
                )
            }
        }
    }

    fun toggleFavoriteRoute(favorite: Favorite, now: Instant) {
        // toggle favorite from User
        ServerState.userList[favorite.userId]?.let { user ->
            if (favorite.userId != null) {
                user.routeFavorites.toMutableMap().let { mutableRouteFavoriteSet ->
                    if (favorite.icon == null && !favorite.favorite) {
                        mutableRouteFavoriteSet.remove(favorite.route)
                    } else {
                        mutableRouteFavoriteSet[favorite.route] = UserFavorite(favorite.favorite, favorite.icon)
                    }
                    ServerState.userList[favorite.userId] = user.copy(
                        routeFavorites = mutableRouteFavoriteSet,
                        updatedOn = now
                    )
                }
            }
        }
    }

    suspend fun putPost(userId: String, messageId: String) = coroutineScope {
        // update user posts
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                posts = user.posts + messageId,
                updatedOn = Clock.System.now()
            )
        }
    }

    fun updateChatForUser(
        dm: DirectMessage,
        userId: String?,
        otherUserId: String?,
        isUser: Boolean,
        now: Instant
    ) =
        ServerState.userList[userId]?.let { user ->
            user.chats.toMutableMap().let { mutableChat ->
                if (otherUserId != null) {
                    mutableChat[otherUserId] = ChatRef(
                        mostRecentDm = dm,
                        chatId = dm.chatId,
                        viewed = !isUser
                    )
                }
                if (userId != null) {
                    ServerState.userList[userId] = user.copy(
                        chats = mutableChat,
                        updatedOn = now
                    )
                }
            }
        }

    fun tagUser(tag: Tag) {
        ServerState.userList[tag.userId]?.let { user ->
            ServerState.userList[tag.userId] = user.copy(
                tag = tag.tag,
                updatedOn = Clock.System.now()
            )
        }
    }

    fun banUser(ban: Ban) {
        ServerState.userList[ban.userId]?.let { user ->
            ServerState.userList[ban.userId] = user.copy(
                banned = true,
                updatedOn = Clock.System.now()
            )
        }
    }
}

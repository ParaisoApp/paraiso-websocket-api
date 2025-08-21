package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.routes.Favorite
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class UsersApi(
    private val usersDBAdapter: UsersDBAdapter
) {
    suspend fun getUserById(userId: String) =
        usersDBAdapter.findById(userId)?.buildUserResponse()

    suspend fun getUserByName(userName: String): UserResponse? =
        usersDBAdapter.findByName(userName)?.buildUserResponse()

    suspend fun exists(search: String) =
        usersDBAdapter.existsByName(search)

    suspend fun getUserByPartial(search: String) =
        usersDBAdapter.findByPartial(search)
            .map { it.buildUserResponse() }

    suspend fun getFollowingById(userId: String) =
        usersDBAdapter.getFollowingById(userId)
            .map { it.buildUserResponse() }

    suspend fun getFollowersById(userId: String) =
        usersDBAdapter.getFollowersById(userId)
            .map { it.buildUserResponse() }

    suspend fun saveUser(user: UserResponse) =
        usersDBAdapter.save(listOf(user.toUser()))

    suspend fun addMentions(userNames: Set<String>, userId: String?, messageId: String): Set<String> = coroutineScope {
        // update user post replies
        val userIds = userNames.map { userName ->
            async {
                usersDBAdapter.addMentionsByName(userName, messageId)
            }
        }.awaitAll().filterNotNull().toMutableSet()
        // if (message.userId != message.userReceiveId) {
        if (userId != null) {
            usersDBAdapter.addMentions(userId, messageId)?.let {
                userIds.add(it)
            }
        }
        userIds.toSet()
    }

    suspend fun setSettings(userId: String, settings: UserSettings) =
        usersDBAdapter.setSettings(userId, settings)

    suspend fun markNotifsRead(userId: String, userNotifs: UserNotifs) =
        usersDBAdapter.markNotifsRead(userId, userNotifs.userChatIds, userNotifs.replyIds)

    suspend fun markReportNotifsRead(userId: String, userReportNotifs: UserReportNotifs) =
        usersDBAdapter.markReportNotifsRead(userId, userReportNotifs.userIds, userReportNotifs.postIds)

    suspend fun addUserReport(report: Report) =
        usersDBAdapter.addUserReport(report.id)

    suspend fun addPostReport(report: Report)  =
        usersDBAdapter.addPostReport(report.id)

    suspend fun follow(follow: Follow) = coroutineScope {
        // add follower to followers list of followee user
        launch {
            usersDBAdapter.findById(follow.followeeId)?.let { followee ->
                if (followee.followers.contains(follow.followerId)) {
                    usersDBAdapter.removeFollowers(followee.id, follow.followerId)
                } else {
                    usersDBAdapter.addFollowers(followee.id, follow.followerId)
                }
            }
        }
        usersDBAdapter.findById(follow.followerId)?.let { follower ->
            if (follower.following.contains(follow.followerId)) {
                usersDBAdapter.removeFollowing(follower.id, follow.followeeId)
            } else {
                usersDBAdapter.addFollowing(follower.id, follow.followeeId)
            }
        }
    }

    suspend fun toggleBlockUser(userId: String, userBlockId: String) {
        usersDBAdapter.findById(userId)?.let { user ->
            if (user.blockList.contains(userBlockId)) {
                usersDBAdapter.removeFromBlocklist(user.id, userBlockId)
            } else {
                usersDBAdapter.addToBlocklist(user.id, userBlockId)
            }
        }
    }

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        // toggle favorite from User
        if(favorite.userId != null) {
            usersDBAdapter.findById(favorite.userId)?.let { user ->
                if (favorite.icon == null && !favorite.favorite) {
                    usersDBAdapter.removeFavoriteRoute(favorite.userId, favorite.route)
                } else {
                    usersDBAdapter.addFavoriteRoute(
                        favorite.userId,
                        favorite.route,
                        UserFavorite(favorite.favorite, favorite.icon)
                    )
                }
            }
        }
    }

    suspend fun putPost(userId: String, messageId: String) =
        usersDBAdapter.addPost(userId, messageId)

    suspend fun updateChatForUser(
        dm: DirectMessage,
        userId: String?,
        otherUserId: String?,
        isUser: Boolean,
    ) {
        if (userId != null && otherUserId != null) {
            usersDBAdapter.setUserChat(
                userId,
                otherUserId,
                ChatRef(
                    mostRecentDm = dm,
                    chatId = dm.chatId,
                    viewed = !isUser
                )
            )
        }
    }
    suspend fun tagUser(tag: Tag) =
        usersDBAdapter.setUserTag(tag)
    suspend fun banUser(ban: Ban) =
        usersDBAdapter.setUserBanned(ban)
}

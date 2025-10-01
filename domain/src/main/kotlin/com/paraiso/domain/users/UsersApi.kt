package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.FollowResponse
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.VoteResponse
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.routes.Favorite
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UsersApi(
    private val usersDB: UsersDB
) {
    suspend fun saveUser(user: UserResponse) =
        usersDB.save(listOf(user.toUser()))

    suspend fun addMentions(
        userNames: Set<String>,
        messageUserReceiveId: String?,
        messageId: String,
        messageUserId: String?
    ): Set<String> = coroutineScope {
        // update user post replies
        val userIds = userNames.map { userName ->
            async {
                usersDB.addMentionsByName(userName, messageId)
            }
        }.awaitAll().filterNotNull().toMutableSet()
        if (messageUserReceiveId != null && messageUserReceiveId != messageUserId) {
            usersDB.addMentions(messageUserReceiveId, messageId)?.let {
                userIds.add(it)
            }
        }
        userIds.toSet()
    }

    suspend fun setSettings(userId: String, settings: UserSettings) =
        usersDB.setSettings(userId, settings)

    suspend fun markNotifsRead(userId: String, userNotifs: UserNotifs) =
        usersDB.markNotifsRead(userId, userNotifs.userChatIds, userNotifs.replyIds)

    suspend fun markReportNotifsRead(userId: String, userReportNotifs: UserReportNotifs) =
        usersDB.markReportNotifsRead(userId, userReportNotifs.userIds, userReportNotifs.postIds)

    suspend fun addUserReport(report: Report) =
        usersDB.addUserReport(report.id)

    suspend fun addPostReport(report: Report)  =
        usersDB.addPostReport(report.id)

    suspend fun follow(follow: FollowResponse) = coroutineScope {
        // add follower to followers list of followee user
        launch {
            usersDB.findById(follow.followeeId)?.let { followee ->
                if (follow.following) {
                    usersDB.setFollowers(followee.id, -1)
                } else {
                    usersDB.setFollowers(followee.id, 1)
                }
            }
        }
        usersDB.findById(follow.followerId)?.let { follower ->
            if (follow.following) {
                usersDB.setFollowing(follower.id, -1)
            } else {
                usersDB.setFollowing(follower.id, 1)
            }
        }
    }

    suspend fun toggleBlockUser(userId: String, userBlockId: String) {
        usersDB.findById(userId)?.let { user ->
            if (user.blockList.contains(userBlockId)) {
                usersDB.removeFromBlocklist(user.id, userBlockId)
            } else {
                usersDB.addToBlocklist(user.id, userBlockId)
            }
        }
    }

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        // toggle favorite from User
        if(favorite.userId != null) {
            usersDB.findById(favorite.userId)?.let { user ->
                if (favorite.icon == null && !favorite.favorite) {
                    usersDB.removeFavoriteRoute(favorite.userId, favorite.route)
                } else {
                    usersDB.addFavoriteRoute(
                        favorite.userId,
                        favorite.route,
                        UserFavorite(favorite.favorite, favorite.icon)
                    )
                }
            }
        }
    }

    suspend fun votePost(userId: String, score: Int) =
        usersDB.setScore(userId, score)

    suspend fun updateChatForUser(
        dm: DirectMessage,
        userId: String?,
        otherUserId: String?,
        isUser: Boolean,
    ) {
        if (userId != null && otherUserId != null) {
            usersDB.setUserChat(
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
        usersDB.setUserTag(tag)
    suspend fun banUser(ban: Ban) =
        usersDB.setUserBanned(ban)
}

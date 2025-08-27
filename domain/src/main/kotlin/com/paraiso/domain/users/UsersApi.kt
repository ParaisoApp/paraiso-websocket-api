package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.messageTypes.Report
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.routes.Favorite
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UsersApi(
    private val usersDB: UsersDB
) {
    suspend fun getUserById(userId: String) =
        usersDB.findById(userId)?.toResponse()

    suspend fun getUserByName(userName: String): UserResponse? =
        usersDB.findByName(userName)?.toResponse()

    suspend fun exists(search: String) =
        usersDB.existsByName(search)

    suspend fun getUserByPartial(search: String) =
        usersDB.findByPartial(search)
            .map { it.toResponse() }

    suspend fun getFollowingById(userId: String) =
        usersDB.getFollowingById(userId)
            .map { it.toResponse() }

    suspend fun getFollowersById(userId: String) =
        usersDB.getFollowersById(userId)
            .map { it.toResponse() }

    suspend fun saveUser(user: UserResponse) =
        usersDB.save(listOf(user.toUser()))

    suspend fun addMentions(userNames: Set<String>, userId: String?, messageId: String): Set<String> = coroutineScope {
        // update user post replies
        val userIds = userNames.map { userName ->
            async {
                usersDB.addMentionsByName(userName, messageId)
            }
        }.awaitAll().filterNotNull().toMutableSet()
        // if (message.userId != message.userReceiveId) {
        if (userId != null) {
            usersDB.addMentions(userId, messageId)?.let {
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

    suspend fun follow(follow: Follow) = coroutineScope {
        // add follower to followers list of followee user
        launch {
            usersDB.findById(follow.followeeId)?.let { followee ->
                if (followee.followers.contains(follow.followerId)) {
                    usersDB.removeFollowers(followee.id, follow.followerId)
                } else {
                    usersDB.addFollowers(followee.id, follow.followerId)
                }
            }
        }
        usersDB.findById(follow.followerId)?.let { follower ->
            if (follower.following.contains(follow.followerId)) {
                usersDB.removeFollowing(follower.id, follow.followeeId)
            } else {
                usersDB.addFollowing(follower.id, follow.followeeId)
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

    suspend fun putPost(userId: String, messageId: String) =
        usersDB.addPost(userId, messageId)

    suspend fun votePost(vote: Vote) =
        vote.voteeId?.let { voteeId ->
            usersDB.findById(voteeId)?.posts?.let { posts ->
                if(
                    posts[vote.postId]?.get(vote.voterId) == vote.upvote
                ){
                    usersDB.removeVotes(voteeId, vote.postId, vote.voterId)
                }else{
                    usersDB.addVotes(voteeId, vote.postId, vote.voterId, vote.upvote)
                }
            }
        }

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

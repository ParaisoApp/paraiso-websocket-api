package com.paraiso.domain.users

import com.paraiso.domain.follows.FollowResponse
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.Tag
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
        messageUserId: String?
    ): Set<String> = coroutineScope {
        // update user post replies
        val userIds = userNames.map { userName ->
            async {
                usersDB.addMentionsByName(userName)
            }
        }.awaitAll().filterNotNull().toMutableSet()
        if (messageUserReceiveId != null && messageUserReceiveId != messageUserId) {
            usersDB.addMentions(messageUserReceiveId)?.let {
                userIds.add(it)
            }
        }
        userIds.toSet()
    }

    suspend fun setSettings(userId: String, settings: UserSettings) =
        usersDB.setSettings(userId, settings)

    suspend fun markNotifsRead(userId: String) =
        usersDB.markNotifsRead(userId)

    suspend fun markReportsRead(userId: String) =
        usersDB.markReportsRead(userId)

    suspend fun addReport() =
        usersDB.addReport()

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

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        // toggle favorite from User
        if (favorite.userId != null) {
            usersDB.findById(favorite.userId)?.let { user ->
                if (favorite.icon == null && !favorite.favorite) {
                    usersDB.removeFavoriteRoute(user.id, favorite.routeId)
                } else {
                    usersDB.addFavoriteRoute(
                        user.id,
                        favorite.routeId,
                        UserFavorite(
                            favorite.routeId,
                            favorite.route,
                            favorite.modifier,
                            favorite.title,
                            favorite.favorite,
                            favorite.icon,
                        )
                    )
                }
            }
        }
    }

    suspend fun votePost(userId: String, score: Int) =
        usersDB.setScore(userId, score)

    suspend fun addChat(
        userId: String?,
    ) =
        userId?.let{
            usersDB.addChat(it)
        }
    suspend fun tagUser(tag: Tag) =
        usersDB.setUserTag(tag)
    suspend fun banUser(ban: Ban) =
        usersDB.setUserBanned(ban)
}

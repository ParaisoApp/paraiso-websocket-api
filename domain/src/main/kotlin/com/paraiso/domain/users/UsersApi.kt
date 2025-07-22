package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UsersApi {
    fun getUserById(userId: String) =
        ServerState.userList[userId]?.buildUserResponse()

    fun getUserByName(userName: String) =
        ServerState.userList.values.find { it.name == userName }?.buildUserResponse()

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

    fun setSettings(userId: String, settings: UserSettings) =
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                settings = settings,
                updatedOn = Clock.System.now()
            )
        }

    fun getFollowingById(userId: String) =
        ServerState.userList.values.filter {
            it.following.contains(userId)
        }.map { it.buildUserResponse() }

    fun getFollowersById(userId: String) =
        ServerState.userList.values.filter {
            it.followers.contains(userId)
        }.map { it.buildUserResponse() }

    fun getUserChat(userId: String) =
        ServerState.userList[userId]?.let { user ->
            user.chats
                .asSequence()
                .associate { chat ->
                    chat.key to UserChat(
                        ServerState.userList[chat.key]?.buildUserResponse(),
                        chat.value.associateBy { it.id ?: UNKNOWN }
                    )
                }
        } ?: emptyMap()

    private fun updateChatForUser(dm: DirectMessage, user: User, otherUserId: String, now: Instant) {
        user.chats.toMutableMap().apply {
            put(otherUserId, getOrDefault(otherUserId, emptySet()) + dm)
        }.let { updatedChats ->
            ServerState.userList[user.id] = user.copy(
                chats = updatedChats,
                updatedOn = now
            )
        }
    }

    fun putDM(dm: DirectMessage) {
        val now = Clock.System.now()
        ServerState.userList[dm.userId]?.let { user ->
            updateChatForUser(dm, user, dm.userReceiveId, now) // update chat for sending user
        }
        ServerState.userList[dm.userReceiveId]?.let { user ->
            updateChatForUser(dm, user, dm.userId, now) // update chat for receiving user
        }
    }

    fun markReplyRead(userId: String, replyId: String) =
        // update user post replies
        ServerState.userList[userId]?.let { user ->
            val now = Clock.System.now()
            user.replies.toMutableMap().let{ mutableReplies ->
                mutableReplies[replyId] = true
                ServerState.userList[userId] = user.copy(
                    replies = mutableReplies,
                    updatedOn = now
                )
            }
        }

    fun markUserChatRead(userId: String, chatId: String, dmId: String) =
        // update user post replies
        ServerState.userList[userId]?.let { user ->
            val now = Clock.System.now()
            user.chats.toMutableMap().let { mutableChats ->
                user.chats[chatId]?.find { it.id == dmId }?.let{foundDm ->
                    user.chats[chatId]?.toMutableSet()?.let {mutableDms ->
                        mutableDms.add(foundDm.copy(viewed = true))
                        mutableChats[userId] = mutableDms
                        ServerState.userList[userId] = user.copy(
                            chats = mutableChats,
                            updatedOn = now
                        )
                    }
                }
            }
        }

    fun follow(follow: Follow) {
        val now = Clock.System.now()
        // add follower to followers list of followee user
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
}

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

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

    fun getOrPutUserChat(chatId: String, userId: String, otherUserId: String) =
        ServerState.userChatList[chatId]?.toReturn() ?: run {
            Clock.System.now().let { now ->
                val user = ServerState.userList[userId]
                val otherUser = ServerState.userList[otherUserId]
                if(user != null && otherUser != null){
                    UUID.randomUUID().toString().let { newChatId ->
                        val newUserChat = UserChat(
                            id = newChatId,
                            users = setOf(user, otherUser),
                            dms = emptySet(),
                            createdOn = now,
                            updatedOn = now
                        )
                        ServerState.userChatList[newChatId] = newUserChat
                        newUserChat.toReturn()
                    }
                }else{
                    null
                }
            }
        }

    private fun updateChatForUser(
        dm: DirectMessage,
        userId: String,
        otherUserId: String,
        isUser: Boolean,
        now: Instant
    ) =
        ServerState.userList[userId]?.let{ user ->
            user.chats.toMutableMap().let { mutableChat ->
                mutableChat[otherUserId] = ChatRef(
                    mostRecentDm = dm,
                    chatId = dm.chatId,
                    viewed = !isUser
                )
                ServerState.userList[userId] = user.copy(
                    chats = mutableChat,
                    updatedOn = now
                )
            }
        }

    fun putDM(dm: DirectMessage) =
        Clock.System.now().let{ now ->
            updateChatForUser(dm, dm.userId, dm.userReceiveId, true, now) // update chat for receiving user
            updateChatForUser(dm, dm.userReceiveId, dm.userId, false, now) // update chat for receiving user
            val test = ServerState.userChatList[dm.chatId]
            println(test)
            ServerState.userChatList[dm.chatId]?.let{chat ->
                ServerState.userChatList[dm.chatId] = chat.copy(
                    dms = chat.dms + dm.copy(
                        createdOn = now
                    ),
                    updatedOn = now
                )
            }
        }

    fun markNotifsRead(userId: String, userNotifs: UserNotifs) =
        ServerState.userList[userId]?.let{user ->
            val now = Clock.System.now()
            //grab chats and make mutable
            user.chats.filter { userNotifs.userChatIds.contains(it.key) }.toMutableMap().let { mutableChats ->
                //find chat and set to true
                mutableChats.map {
                    mutableChats[it.key] = it.value.copy(
                        viewed = true
                    )
                }
                //grab chats and make mutable
                user.replies.filter { userNotifs.replyIds.contains(it.key) }.toMutableMap().let { mutableReplies ->
                    //find chat and set to true
                    mutableReplies.map {
                        mutableReplies[it.key] = true
                    }
                    //update user
                    ServerState.userList[userId] = user.copy(
                        chats = mutableChats,
                        replies = mutableReplies,
                        updatedOn = now
                    )
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

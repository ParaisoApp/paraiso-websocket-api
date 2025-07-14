package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.Follow
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UsersApi {
    fun getUserById(userId: String) =
        ServerState.userList[userId]?.let { user -> buildUser(user) }

    fun getUserByName(userName: String) =
        ServerState.userList.values.find { it.name == userName }?.let { user -> buildUser(user) }

    fun getUserList() =
        ServerState.userList.values
            .filter { it.status != UserStatus.DISCONNECTED }
            .associate { user -> user.id to buildUser(user) }

    fun setSettings(userId: String, settings: UserSettings) =
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                settings = settings,
                updatedOn = Clock.System.now()
            )
        }

    fun getUserChat(userId: String) =
        ServerState.userList[userId]?.let { user ->
            user.chats
                .asSequence()
                .associate { chat ->
                    chat.key to UserChat(
                        ServerState.userList[chat.key]?.let { user -> buildUser(user) },
                        chat.value.associateBy { it.id ?: UNKNOWN }
                    )
                }
        } ?: emptyMap()

    private fun updateChatForUser(dm: DirectMessage, user: User, otherUserId: String, now: Instant) {
        user.chats.toMutableMap().apply {
            put(otherUserId, getOrDefault(otherUserId, emptyList()) + dm)
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

    fun follow(follow: Follow) {
        val now = Clock.System.now()
        // add session user to followers list of input user
        ServerState.userList[follow.userId]?.let { user ->
            user.followers.toMutableSet().apply {
                if(user.followers.contains(follow.sessionUserId)){
                    remove(follow.sessionUserId)
                } else {
                    add(follow.sessionUserId)
                }
            }.let { updatedFollowing ->
                ServerState.userList[user.id] = user.copy(
                    following = updatedFollowing,
                    updatedOn = now
                )
            }
        }
        // add input user to following of session user
        ServerState.userList[follow.sessionUserId]?.let { sessionUser ->
            sessionUser.following.toMutableSet().apply {
                if(sessionUser.following.contains(follow.userId)){
                    remove(follow.userId)
                } else {
                    add(follow.userId)
                }
            }.let { updatedFollowing ->
                ServerState.userList[sessionUser.id] = sessionUser.copy(
                    following = updatedFollowing,
                    updatedOn = now
                )
            }
        }
    }
}

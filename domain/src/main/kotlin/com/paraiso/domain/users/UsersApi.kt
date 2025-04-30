package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UsersApi {
    fun getUserById(userId: String) =
        ServerState.userList[userId]?.let { user -> buildUser(user) }

    fun getUserList() =
        ServerState.userList.values.associate { user -> user.id to buildUser(user) }

    private fun buildUser(user: User) =
        user.let {
            val posts = mutableMapOf<String, Map<String, Boolean>>()
            val comments = mutableMapOf<String, Map<String, Boolean>>()
            ServerState.posts
                .filterKeys { user.posts.contains(it) }
                .values
                .map { post ->
                    if (post.type == PostType.SUB) {
                        posts[post.id] = post.votes
                    } else {
                        comments[post.id] = post.votes
                    }
                }
            user.toUserReturn(posts, comments)
        }


    fun setSettings(userId: String, settings: UserSettings) =
        ServerState.userList[userId]?.let { user ->
            ServerState.userList[userId] = user.copy(
                settings = settings,
                updatedOn = Clock.System.now()
            )
        }

    private fun updateChatForUser(dm: DirectMessage, user: User, otherUserId: String, now: Instant) {
        user.chats.toMutableMap().apply{
            put(otherUserId, getOrDefault(otherUserId, emptyList()) + dm)
        }.let{updatedChats ->
            ServerState.userList[user.id] = user.copy(
                chats = updatedChats,
                updatedOn = now
            )
        }
    }

    fun putDM(dm: DirectMessage) {
        val now = Clock.System.now()
        ServerState.userList[dm.userId]?.let { user ->
            updateChatForUser(dm, user, dm.userReceiveId, now) //update chat for sending user
        }
        ServerState.userList[dm.userReceiveId]?.let { user ->
            updateChatForUser(dm, user, dm.userId, now) //update chat for receiving user
        }
    }
}

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.PostType
import com.paraiso.domain.util.ServerState

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
            ServerState.userList[userId] = user.copy(settings = settings)
        }
}

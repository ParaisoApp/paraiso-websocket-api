package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.PostType
import com.paraiso.domain.util.ServerState

class UsersApi {
    fun getUserList() =
        ServerState.userList.values.map { user ->
            val posts = mutableMapOf<String, Map<String, Boolean>>()
            val comments = mutableMapOf<String, Map<String, Boolean>>()
            ServerState.posts
                .filterKeys { user.posts.contains(it) }
                .values
                .map { post ->
                    if(post.type == PostType.SUB){
                        posts[post.id] = post.votes
                    }else{
                        comments[post.id] = post.votes
                    }
                }
            user.toUserReturn(posts, comments)
        }
}
package com.paraiso.database.posts

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostsDBAdapter

class PostsDBAdapterImpl(database: MongoDatabase): PostsDBAdapter {
    private val collection = database.getCollection("posts", Post::class.java)
}
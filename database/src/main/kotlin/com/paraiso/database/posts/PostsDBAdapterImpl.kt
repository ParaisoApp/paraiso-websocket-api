package com.paraiso.database.posts

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostsDBAdapter
import kotlinx.coroutines.flow.firstOrNull

class PostsDBAdapterImpl(database: MongoDatabase): PostsDBAdapter {
    private val collection = database.getCollection("posts", Post::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(Post::id.name, id)).firstOrNull()

    suspend fun save(posts: List<Post>) =
        collection.insertMany(posts)
}
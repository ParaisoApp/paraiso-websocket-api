package com.paraiso.database.posts

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostsDBAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class PostsDBAdapterImpl(database: MongoDatabase): PostsDBAdapter {
    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 5
    }

    private val collection = database.getCollection("posts", Post::class.java)

    suspend fun findById(id: String) =
        collection.find(eq(Post::id.name, id)).firstOrNull()

    suspend fun findByIds(ids: Set<String>) =
        collection.find(`in`(Post::id.name, ids))

    suspend fun findByPartial(partial: String): FindFlow<Post> =
        collection.find(Filters.regex(Post::title.name, partial, "i")).limit(PARTIAL_RETRIEVE_LIM)

    suspend fun save(posts: List<Post>) =
        collection.insertMany(posts)

    suspend fun setVotes(
        id: String,
        votes:  Map<String, Boolean>
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.set(Post::votes.name, votes),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setPostDeleted(
        id: String
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.set(Post::status.name, PostStatus.DELETED),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setCount(
        id: String,
        increment: Int // +1 for inc | -1 for dec
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.inc("count", 1 * increment),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )
}
package com.paraiso.database.posts

import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.util.fieldsEq
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostsDBAdapter
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.USER_PREFIX
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bson.Document
import org.bson.conversions.Bson

class PostsDBAdapterImpl(database: MongoDatabase): PostsDBAdapter {
    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 5
        const val TIME_WEIGHTING = 10000000000
    }

    private val collection = database.getCollection("posts", Post::class.java)

    suspend fun findById(id: String) =
        collection.find(eq(Post::id.name, id)).firstOrNull()

    private fun getInitAggPipeline(
        initialFilter: Bson,
    ) =
        mutableListOf(
            // Step 1: apply initial filters
            match(initialFilter),

            // Step 2: join user roles
            lookup(
                "users",   // from collection
                Post::userId.name,        // localField
                User::id.name,     // foreignField
                "userInfo"    // as
            ),
            limit(RETRIEVE_LIM)
        )

    private fun getUserRoleCondition(
        filters: FilterTypes,
        userFollowing: Set<String>
    ): Bson {
        val roleCondition = Document("\$gt", listOf(
            Document("\$size",
                Document("\$setIntersection", listOf("\$userInfo.roles", filters.userRoles))
            ), 0
        ))

        val orConditions = mutableListOf(roleCondition)

        // Only add following condition if FOLLOWING is in filters
        if (filters.userRoles.contains(UserRole.FOLLOWING)) {
            val followingCondition = Document("\$in", listOf("\$userId", userFollowing))
            orConditions.add(followingCondition)
        }
        return match(Document("\$expr", Document("\$or", orConditions)))
    }
    private fun getSort(
        sortType: SortType,
    ) =
        when(sortType) {
            SortType.NEW -> Document("\$sort", Document(Post::createdOn.name, -1))
            SortType.TOP -> Document("\$sort", Document(
                "score", Document("\$sum", listOf(
                    Document("\$map", Document()
                        .append("input", Document("\$objectToArray", "\$votes"))
                        .append("as", "vote")
                        .append("in", Document("\$cond", listOf("\$\$vote.v", 1, -1)))
                    )
                ))
            ))
            SortType.HOT -> Document("\$sort", Document(
                "\$multiply", listOf(
                    Document("\$divide", listOf("\$createdOn", TIME_WEIGHTING)),
                    Document(
                        "\$sum", listOf(
                            Document("\$map", Document()
                                .append("input", Document("\$objectToArray", "\$votes"))
                                .append("as", "vote")
                                .append("in", Document("\$cond", listOf("\$\$vote.v", 1, -1)))
                            )
                        )
                    )
                )
            ))
        }

    suspend fun findByBaseCriteria(
        postSearchId: String,
        range: Instant,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ) = coroutineScope {
        val homeFilters = mutableListOf(
            eq(Post::userId.name, postSearchId.removePrefix(USER_PREFIX))
        )
        if(postSearchId == SiteRoute.HOME.name){
            homeFilters.add(fieldsEq(Post::rootId, Post::id))
        }

        val orConditions = mutableListOf<Bson>()
        orConditions += eq(Post::parentId.name, postSearchId)
        orConditions += eq(Post::userId.name, postSearchId.removePrefix(USER_PREFIX))
        orConditions += and(homeFilters)

        val initialFilter = and(
            or(orConditions),
            gt(Post::createdOn.name, range),
            ne(Post::status.name, PostStatus.DELETED),
            `in`(Post::type.name, filters.postTypes),
        )

        val pipeline = getInitAggPipeline(initialFilter)
        pipeline.add(getUserRoleCondition(filters, userFollowing))
        pipeline.add(getSort(sortType))

        collection.aggregate<Post>(pipeline)
    }

    suspend fun findBySubpostIds(
        ids: Set<String>,
        range: Instant,
        sortType: SortType,
        filters: FilterTypes,
        subPostIds: Set<String>,
        userFollowing: Set<String>
    ) = coroutineScope {
        collection.find(`in`(Post::id.name, ids))
            .limit(RETRIEVE_LIM)

        val initialFilter = and(
            `in`(Post::id.name, subPostIds),
            gt(Post::createdOn.name, range),
            ne(Post::status.name, PostStatus.DELETED),
            `in`(Post::type.name, filters.postTypes),
        )

        val pipeline = getInitAggPipeline(initialFilter)
        pipeline.add(getUserRoleCondition(filters, userFollowing))
        pipeline.add(getSort(sortType))
        collection.aggregate<Post>(pipeline)

    }

    suspend fun findByPartial(partial: String): FindFlow<Post> =
        collection.find(Filters.regex(Post::title.name, partial, "i"))
            .limit(PARTIAL_RETRIEVE_LIM)

    suspend fun save(posts: List<Post>) =
        collection.insertMany(posts)

    suspend fun editPost(
        id: String,
        title: String,
        content: String,
        media: String,
        data: String
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.set(Post::title.name, title),
                Updates.set(Post::content.name, content),
                Updates.set(Post::media.name, media),
                Updates.set(Post::data.name, data),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun updateParent(
        id: String,
        subPostId: String,
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.addToSet(Post::subPosts.name, subPostId),
                Updates.inc(Post::count.name, 1),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setVotes(
        id: String,
        voteUserId: String,
        upvote: Boolean
    ) =
        collection.updateOne(
            eq(Post::id.name, id),
            Updates.combine(
                Updates.set("${Post::votes.name}.$voteUserId", upvote),
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
                Updates.inc(Post::count.name, 1 * increment),
                Updates.set(Post::updatedOn.name, Clock.System.now())
            )
        )
}
package com.paraiso.database.posts

import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.nin
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.util.eqId
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.USER_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bson.Document
import org.bson.conversions.Bson
import java.util.Date
import kotlin.time.Duration.Companion.days

class PostsDBImpl(database: MongoDatabase) : PostsDB {
    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 10
        val TIME_WEIGHTING = 10_000_000_000L
        val RISING_TIME_MULTIPLIER = 2.0
        const val COMMENT_WEIGHTING = 2
    }

    private val collection = database.getCollection("posts", Post::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun findByIdsIn(ids: Set<String>) =
        withContext(Dispatchers.IO) {
            collection.find(`in`(ID, ids)).toList()
        }

    override suspend fun findByPartial(partial: String): List<Post> =
        withContext(Dispatchers.IO) {
            collection.find(
                and(
                    or(
                        regex(Post::title.name, partial, "i"),
                        regex(Post::content.name, partial, "i")
                    ),
                    not(regex(ID, "^TEAM", "i")) // remove team event posts from search
                )
            ).limit(PARTIAL_RETRIEVE_LIM).toList()
        }

    override suspend fun findByUserId(userId: String) =
        withContext(Dispatchers.IO) {
            collection.find(eq(Post::userId.name, userId)).toList()
        }

    private fun getInitAggPipeline(
        initialFilter: Bson
    ) =
        mutableListOf(
            // Step 1: apply initial filters
            match(initialFilter),

            // Step 2: join user roles
            lookup(
                "users", // from collection
                Post::userId.name, // localField
                ID, // foreignField
                "userInfo" // as
            )
        )

    private fun getUserRoleCondition(
        filters: FilterTypes,
        userFollowing: Set<String>
    ): Bson {
        val roleCondition = Document(
            "\$gt",
            listOf(
                Document(
                    "\$size",
                    Document("\$setIntersection", listOf("\$userInfo.roles", filters.userRoles))
                ),
                0
            )
        )

        val orConditions = mutableListOf(roleCondition)

        // Only add following condition if FOLLOWING is in filters
        if (filters.userRoles.contains(UserRole.FOLLOWING)) {
            val followingCondition = Document("\$in", listOf("\$userId", userFollowing))
            orConditions.add(followingCondition)
        }
        return match(Document("\$expr", Document("\$or", orConditions)))
    }

    // (voteSum + 2 * count)
    private fun getScore(): Document {
        return Document(
            "\$add",
            listOf(
                Document(
                    "\$sum",
                    listOf(
                        Document(
                            "\$map",
                            Document()
                                .append("input", Document("\$objectToArray", "\$votes"))
                                .append("as", "vote")
                                .append("in", Document("\$cond", listOf("\$\$vote.v", 1, -1)))
                        )
                    )
                ),
                Document("\$multiply", listOf(COMMENT_WEIGHTING, "\$count"))
            )
        )
    }

    /*
     * weightedScore = sign(s) * log10(max(|s|, 1)) + RISING_MULT * (timestamp(createdOn) / TIME_WEIGHTING)
     * where:
     *   s = sum of votes (+1/-1) + COMMENT_WEIGHTING * count
     *   RISING_MULT = multiplier for boosting newer posts
     *   TIME_WEIGHTING = scaling factor to normalize timestamp
     */
    private fun getTimeAndVoteWeighting(risingMult: Double): Document {
        return Document(
            "\$addFields",
            Document(
                "weightedScore",
                Document(
                    "\$let",
                    Document("vars", Document("s", getScore()))
                        .append(
                            "in",
                            Document(
                                "\$add",
                                listOf(
                                    // sign(s) * log10(max(|s|,1))
                                    Document(
                                        "\$multiply",
                                        listOf(
                                            Document(
                                                "\$cond",
                                                listOf(
                                                    Document("\$gt", listOf("\$\$s", 0)),
                                                    1,
                                                    Document(
                                                        "\$cond",
                                                        listOf(
                                                            Document("\$lt", listOf("\$\$s", 0)),
                                                            -1,
                                                            0
                                                        )
                                                    )
                                                )
                                            ),
                                            Document(
                                                "\$log10",
                                                Document(
                                                    "\$max",
                                                    listOf(
                                                        Document("\$abs", "\$\$s"),
                                                        1
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    // time factor: createdOnTimestamp / TIME_WEIGHTING
                                    Document(
                                        "\$multiply",
                                        listOf(
                                            risingMult,
                                            Document(
                                                "\$divide",
                                                listOf(
                                                    // createdOn is already a date, get milliseconds since epoch
                                                    Document("\$toLong", "\$createdOn"),
                                                    TIME_WEIGHTING
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                )
            )
        )
    }

    private fun getSort(sortType: SortType): List<Bson> {
        return when (sortType) {
            SortType.NEW -> listOf(
                Document("\$sort", Document(Post::createdOn.name, -1))
            )

            SortType.TOP -> listOf(
                Document(
                    "\$addFields",
                    Document(
                        "score",
                        // (voteSum + 2 * count)
                        getScore()
                    )
                ),
                Document("\$sort", Document("score", -1))
            )

            SortType.HOT -> listOf(
                getTimeAndVoteWeighting(1.0),
                Document("\$sort", Document("weightedScore", -1))
            )

            SortType.RISING -> listOf(
                getTimeAndVoteWeighting(RISING_TIME_MULTIPLIER),
                Document("\$sort", Document("weightedScore", -1))
            )
        }
    }

    override suspend fun findByBaseCriteria(
        postSearchId: String,
        basePostName: String,
        range: Instant,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ) =
        withContext(Dispatchers.IO) {
            val homeFilters = mutableListOf(
                eq(Post::userId.name, postSearchId.removePrefix(USER_PREFIX))
            )
            if (postSearchId == HOME_PREFIX) {
                // home page should have all root posts - will eventually resolve to following
                homeFilters.add(
                    and(
                        eqId(Post::rootId),
                        not(regex(ID, "^TEAM", "i")) // remove team event posts unless in team route
                    )
                )
            }
            if (
                basePostName == SiteRoute.BASKETBALL.name ||
                basePostName == SiteRoute.FOOTBALL.name ||
                basePostName == SiteRoute.HOCKEY.name
                ) {
                // for sports add their respective gameposts
                homeFilters.add(
                    and(
                        eq(Post::type.name, PostType.EVENT.name),
                        eq(Post::data.name, basePostName), // route is housed in data field
                        not(regex(ID, "^TEAM", "i")) // remove team event posts unless in team route
                    )
                )
            }

            val orConditions = listOf(
                eq(Post::parentId.name, postSearchId),
                eq(Post::userId.name, postSearchId.removePrefix(USER_PREFIX)),
                or(homeFilters)
            )

            val andConditions = mutableListOf(
                or(orConditions),
                gt(Post::createdOn.name, Date.from(range.toJavaInstant())),
                ne(Post::status.name, PostStatus.DELETED),
                `in`(Post::type.name, filters.postTypes),
                nin(ID, filters.postIds),
                // handle events (which may be created early but create date of event date)
                lte(Post::createdOn.name, Date.from(Clock.System.now().plus(1.days).toJavaInstant()))
            )

            val initialFilter = and(andConditions)

            val pipeline = getInitAggPipeline(initialFilter)
            pipeline.add(getUserRoleCondition(filters, userFollowing))
            pipeline.addAll(getSort(sortType))
            pipeline.add(limit(RETRIEVE_LIM))

            return@withContext collection.aggregate<Post>(pipeline).toList()
        }

    override suspend fun findByParentId(
        parentId: String,
        range: Instant,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ) =
        withContext(Dispatchers.IO) {
            val initialFilter = and(
                eq(Post::parentId.name, parentId),
                gt(Post::createdOn.name, Date.from(range.toJavaInstant())),
                ne(Post::status.name, PostStatus.DELETED),
                `in`(Post::type.name, filters.postTypes)
            )

            val pipeline = getInitAggPipeline(initialFilter)
            pipeline.add(getUserRoleCondition(filters, userFollowing))
            pipeline.addAll(getSort(sortType))
            return@withContext collection.aggregate<Post>(pipeline).toList()
        }

    override suspend fun save(posts: List<Post>) =
        withContext(Dispatchers.IO) {
            val bulkOps = posts.map { post ->
                ReplaceOneModel(
                    eq(ID, post.id),
                    post,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun editPost(message: Message) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, message.id),
                combine(
                    set(Post::title.name, message.title),
                    set(Post::content.name, message.content),
                    set(Post::media.name, message.media),
                    set(Post::data.name, message.data),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setPostDeleted(
        id: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    set(Post::status.name, PostStatus.DELETED),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setScore(
        id: String,
        score: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(Post::score.name, score),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setCount(
        id: String,
        increment: Int // +1 for inc | -1 for dec
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                eq(ID, id),
                combine(
                    inc(Post::count.name, 1 * increment),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
}

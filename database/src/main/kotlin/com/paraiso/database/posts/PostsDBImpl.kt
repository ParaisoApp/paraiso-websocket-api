package com.paraiso.database.posts

import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.nin
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.util.eqId
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.posts.GameState
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.RouteResponse
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.routes.isSportRoute
import com.paraiso.domain.users.UserFavorite
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.FAVORITES_PREFIX
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.SPORT_PREFIX
import com.paraiso.domain.util.Constants.USER_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.conversions.Bson
import java.util.Date

class PostsDBImpl(database: MongoDatabase) : PostsDB {
    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 10
        const val TIME_WEIGHTING = 10_000_000_000L
        const val RISING_TIME_MULTIPLIER = 2.0
        const val COMMENT_WEIGHTING = 0.5
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
        if (filters.userRoles.contains(UserRole.FOLLOWING) && userFollowing.isNotEmpty()) {
            val followingCondition = Document("\$in", listOf("\$userId", userFollowing))
            orConditions.add(followingCondition)
        }
        return match(Document("\$expr", Document("\$or", orConditions)))
    }

    // (voteSum + commentWeight * count)
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
                        "calculatedScore",
                        // (voteSum + commentWeight * count)
                        getScore()
                    )
                ),
                Document("\$sort", Document("calculatedScore", -1))
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
        route: RouteResponse?,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFavorites: List<UserFavorite>,
        userFollowing: Set<String>
    ) =
        withContext(Dispatchers.IO) {
            val routeFilter = when {
                route == null -> null
                // if id == root id then post was made at base route
                route.route == SiteRoute.HOME -> eqId(Post::rootId)
                // Profile case where search id is the user id - match to post's user id
                route.route == SiteRoute.PROFILE -> eq(Post::userId.name, route.modifier)
                // favorites resolve to user's favorites
                route.route == SiteRoute.FAVORITES -> {
                    val favConds = userFavorites.map { fav ->
                        if(isSportRoute(fav.route)){
                            val sportBase = and(eq(Post::data.name, fav.route), eq(Post::type.name, PostType.EVENT.name))
                            val teamFilter = fav.modifier?.let { regex(Post::title.name, it, "i") }
                            or(
                                if (teamFilter != null) and(sportBase, teamFilter) else sportBase,
                                eq(Post::parentId.name, fav.routeId)
                            )
                        }else{
                            //grab all posts in that fav route
                            eq(Post::parentId.name, fav.routeId)
                        }
                    }
                    // add or condition if more than one condition exists
                    when {
                        favConds.isEmpty() -> null
                        favConds.size == 1 -> favConds.first()
                        else -> or(favConds)
                    }
                }
                // grab events for each sport route, add team filter if team route
                route.route in listOf(SiteRoute.BASKETBALL, SiteRoute.FOOTBALL, SiteRoute.HOCKEY) -> {
                    val sportBase = and(eq(Post::data.name, route.route), eq(Post::type.name, PostType.EVENT.name))
                    val teamFilter = route.modifier?.let { regex(Post::title.name, it, "i") }
                    if (teamFilter != null) and(sportBase, teamFilter) else sportBase
                }
                else -> null
            }
            // add or condition if more than one condition exists
            val orConditions = when {
                routeFilter != null -> or(routeFilter, eq(Post::parentId.name, route?.id))
                else -> eq(Post::parentId.name, route?.id)
            }

            val andConditions = mutableListOf(
                orConditions,
                ne(Post::status.name, PostStatus.DELETED),
                `in`(Post::type.name, filters.postTypes),
                nin(ID, filters.postIds),
                // handle events (which may be created early but create date of event date)
                lte(Post::createdOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )

            range?.let {
                andConditions.add(gt(Post::createdOn.name, Date.from(it.toJavaInstant())))
            }

            val initialFilter = and(andConditions)

            val pipeline = getInitAggPipeline(initialFilter)
            pipeline.add(getUserRoleCondition(filters, userFollowing))
            pipeline.addAll(getSort(sortType))
            pipeline.add(limit(RETRIEVE_LIM))

            return@withContext collection.aggregate<Post>(pipeline).toList()
        }

    override suspend fun findByParentId(
        parentId: String,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ) =
        withContext(Dispatchers.IO) {
            val andConditions = mutableListOf(
                eq(Post::parentId.name, parentId),
                ne(Post::status.name, PostStatus.DELETED),
                `in`(Post::type.name, filters.postTypes)
            )
            range?.let {
                andConditions.add(gt(Post::createdOn.name, Date.from(it.toJavaInstant())))
            }
            val initialFilter = and(andConditions)

            val pipeline = getInitAggPipeline(initialFilter)
            pipeline.add(getUserRoleCondition(filters, userFollowing))
            pipeline.addAll(getSort(sortType))
            return@withContext collection.aggregate<Post>(pipeline).toList()
        }

    override suspend fun findByParentIdWithEventFilters(
        parentId: String,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>,
        compStartTime: Instant?,
        compEndTime: Instant?,
        gameState: GameState?,
        commentRouteLocation: String?
    ) =
        withContext(Dispatchers.IO) {
            val buildFilters = mutableListOf(
                eq(Post::parentId.name, parentId),
                ne(Post::status.name, PostStatus.DELETED),
                `in`(Post::type.name, filters.postTypes)
            )
            if (gameState != null && compStartTime != null && compEndTime != null) {
                when (gameState) {
                    GameState.PRE -> {
                        buildFilters.add(lt(Post::createdOn.name, Date.from(compStartTime.toJavaInstant())))
                    }
                    GameState.MID -> {
                        buildFilters.add(gt(Post::createdOn.name, Date.from(compStartTime.toJavaInstant())))
                        buildFilters.add(lt(Post::createdOn.name, Date.from(compEndTime.toJavaInstant())))
                    }
                    GameState.POST -> {
                        buildFilters.add(gt(Post::createdOn.name, Date.from(compEndTime.toJavaInstant())))
                    }
                    GameState.ALL -> {}
                }
            }
            commentRouteLocation?.let {
                buildFilters.add(eq(Post::route.name, commentRouteLocation))
            }
            range?.let {
                buildFilters.add(gt(Post::createdOn.name, Date.from(it.toJavaInstant())))
            }
            val initialFilter = and(buildFilters)

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

    override suspend fun saveIfNew(posts: List<Post>) =
        withContext(Dispatchers.IO) {
            val bulkOps = posts.map { post ->
                val doc = Document.parse(Json.encodeToString(post))
                UpdateOneModel<Post>(
                    eq("_id", post.id),
                    Updates.setOnInsert(doc),
                    UpdateOptions().upsert(true)
                )
            }
            val result = collection.bulkWrite(bulkOps, BulkWriteOptions().ordered(false))
            result.insertedCount
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

package com.paraiso.database.posts

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.nin
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Sorts.orderBy
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.util.eqId
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.messageTypes.calculateScores
import com.paraiso.domain.posts.ActiveStatus
import com.paraiso.domain.posts.Cursor
import com.paraiso.domain.posts.GameState
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.routes.isSportRoute
import com.paraiso.domain.users.UserFavorite
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.ACTIVE_SPORTS
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bson.conversions.Bson
import java.util.Date
import com.paraiso.domain.posts.Post as PostDomain

class PostsDBImpl(database: MongoDatabase) : PostsDB, Klogging {
    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 10
    }

    private val collection = database.getCollection("posts", Post::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try {
                collection.find(
                    and(
                        eq(ID, id),
                        eq(Post::status.name, ActiveStatus.ACTIVE)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception) {
                logger.error { "Error finding post by id: $ex" }
                null
            }
        }

    override suspend fun findByIdsIn(ids: Set<String>) =
        withContext(Dispatchers.IO) {
            try {
                collection.find(
                    and(
                        `in`(ID, ids),
                        eq(Post::status.name, ActiveStatus.ACTIVE)
                    )
                ).map { it.toDomain() }.toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun findByPartial(partial: String) =
        withContext(Dispatchers.IO) {
            try {
                collection.find(
                    and(
                        or(
                            regex(Post::title.name, partial, "i"),
                            regex(Post::content.name, partial, "i")
                        ),
                        not(regex(ID, "^TEAM", "i")), // remove team event posts from search
                        eq(Post::status.name, ActiveStatus.ACTIVE)
                    )
                ).limit(PARTIAL_RETRIEVE_LIM).map { it.toDomain() }.toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by partial: $ex" }
                emptyList()
            }
        }

    override suspend fun findByUserId(userId: String) =
        withContext(Dispatchers.IO) {
            try {
                collection.find(
                    and(
                        eq(Post::userId.name, userId),
                        eq(Post::status.name, ActiveStatus.ACTIVE)
                    )
                ).map { it.toDomain() }.toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by user id: $ex" }
                emptyList()
            }
        }

    private fun getSort(sortType: SortType) =
        when (sortType) {
            SortType.NEW -> descending(Post::createdOn.name)
            SortType.TOP -> descending(Post::topScore.name)
            SortType.HOT -> descending(Post::hotScore.name)
            SortType.RISING -> descending(Post::risingScore.name)
        }.let { sort ->
            // tiebreak the sort against the ID in case of equal values (for pagination)
            orderBy(sort, descending((ID)))
        }

    private fun getCursorFilter(sortType: SortType, cursor: Cursor) =
        when (sortType) {
            SortType.NEW -> or(
                lt(Post::createdOn.name, Date.from(cursor.date.toJavaInstant())),
                and(
                    eq(Post::createdOn.name, Date.from(cursor.date.toJavaInstant())),
                    lt(ID, cursor.id)
                )
            )
            SortType.TOP -> or(
                lt(Post::topScore.name, cursor.score),
                and(
                    eq(Post::topScore.name, cursor.score),
                    lt(ID, cursor.id)
                )
            )
            SortType.HOT -> or(
                lt(Post::hotScore.name, cursor.score),
                and(
                    eq(Post::hotScore.name, cursor.score),
                    lt(ID, cursor.id)
                )
            )
            SortType.RISING -> or(
                lt(Post::risingScore.name, cursor.score),
                and(
                    eq(Post::risingScore.name, cursor.score),
                    lt(ID, cursor.id)
                )
            )
        }

    private fun getBaseFilters(
        range: Instant?,
        filters: FilterTypes,
        userFollowing: Set<String>
    ) = mutableListOf(
        eq(Post::status.name, ActiveStatus.ACTIVE),
        `in`(Post::type.name, filters.postTypes),
        `in`(Post::userRole.name, filters.userRoles)
    ).apply {
        if (filters.userRoles.contains(UserRole.FOLLOWING) && userFollowing.isNotEmpty()) {
            add(`in`(Post::userId.name, userFollowing))
        }
        range?.let {
            add(gt(Post::createdOn.name, Date.from(it.toJavaInstant())))
        }
    }
    private fun getRouteFilter(
        route: RouteDetails?,
        userFavorites: List<UserFavorite>
    ) = when {
        route == null -> null
        // if id == root id then post was made at base route
        route.route == SiteRoute.HOME -> eqId(Post::rootId)
        // Profile case where search id is the user id - match to post's user id
        route.route == SiteRoute.PROFILE -> eq(Post::userId.name, route.modifier)
        // favorites resolve to user's favorites
        route.route == SiteRoute.FAVORITES -> {
            val parentIds = mutableListOf<String>()
            val sportLeagues = mutableListOf<String>()
            val teamConditions = mutableListOf<Bson>()
            userFavorites.forEach { fav ->
                fav.routeId.let { parentIds.add(it) }
                if (isSportRoute(fav.route)) {
                    if (fav.altId != null) {
                        // Specific team filter within a sport (requires isolated pairing)
                        teamConditions.add(
                            and(
                                eq(Post::data.name, fav.route),
                                eq(Post::type.name, PostType.EVENT.name),
                                eq(Post::tags.name, fav.altId)
                            )
                        )
                    } else {
                        // no team specific so safe to batch full sport league
                        sportLeagues.add(fav.route)
                    }
                }
            }
            val consolidatedClauses = mutableListOf<Bson>()
            if (parentIds.isNotEmpty()) {
                consolidatedClauses.add(`in`(Post::parentId.name, parentIds))
            }
            if (sportLeagues.isNotEmpty()) {
                consolidatedClauses.add(
                    and(
                        `in`(Post::data.name, sportLeagues),
                        eq(Post::type.name, PostType.EVENT.name)
                    )
                )
            }
            if (teamConditions.isNotEmpty()) {
                consolidatedClauses.addAll(teamConditions)
            }
            // add or condition if more than one condition exists
            when {
                consolidatedClauses.isEmpty() -> null
                consolidatedClauses.size == 1 -> consolidatedClauses.first()
                else -> or(consolidatedClauses)
            }
        }
        // General sports case
        route.route == SiteRoute.SPORT -> {
            or(
                and(
                    `in`(Post::data.name, ACTIVE_SPORTS),
                    eq(Post::type.name, PostType.EVENT.name)
                ),
                `in`(Post::parentId.name, ACTIVE_SPORTS)
            )
        }
        // grab events for each sport route, add team filter if team route
        route.route in ACTIVE_SPORTS -> {
            val sportBase = and(eq(Post::data.name, route.route), eq(Post::type.name, PostType.EVENT.name))
            val teamFilter = route.altId?.let { eq(Post::tags.name, it) }
            if (teamFilter != null) and(sportBase, teamFilter) else sportBase
        }
        else -> null
    }

    override suspend fun findByBaseCriteria(
        route: RouteDetails?,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFavorites: List<UserFavorite>,
        userFollowing: Set<String>,
        cursor: Cursor?
    ) =
        withContext(Dispatchers.IO) {
            val routeFilter = getRouteFilter(route, userFavorites)
            // add or condition if more than one condition exists
            val orConditions = when {
                routeFilter != null -> or(routeFilter, eq(Post::parentId.name, route?.id))
                else -> eq(Post::parentId.name, route?.id)
            }

            val baseFilters = getBaseFilters(range, filters, userFollowing).apply {
                add(orConditions)
                add(nin(ID, filters.postIds))
                add(lte(Post::createdOn.name, Date.from(Clock.System.now().toJavaInstant())))
                if(cursor != null) add(getCursorFilter(sortType, cursor))
            }

            try {
                return@withContext collection.find(and(baseFilters))
                    .sort(getSort(sortType))
                    .limit(RETRIEVE_LIM)
                    .map { it.toDomain() }
                    .toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by base criteria: $ex" }
                return@withContext emptyList()
            }
        }

    override suspend fun findByParentId(
        parentId: String,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ) =
        withContext(Dispatchers.IO) {
            val baseFilters = getBaseFilters(range, filters, userFollowing).apply {
                add(eq(Post::parentId.name, parentId))
            }
            try {
                return@withContext collection.find(and(baseFilters))
                    .sort(getSort(sortType))
                    .limit(RETRIEVE_LIM)
                    .map { it.toDomain() }
                    .toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by parent id: $ex" }
                return@withContext emptyList()
            }
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
            val baseFilters = getBaseFilters(range, filters, userFollowing).apply {
                add(eq(Post::parentId.name, parentId))
                if (gameState != null && compStartTime != null && compEndTime != null) {
                    when (gameState) {
                        GameState.PRE -> {
                            add(lt(Post::createdOn.name, Date.from(compStartTime.toJavaInstant())))
                        }
                        GameState.MID -> {
                            add(gt(Post::createdOn.name, Date.from(compStartTime.toJavaInstant())))
                            add(lt(Post::createdOn.name, Date.from(compEndTime.toJavaInstant())))
                        }
                        GameState.POST -> {
                            add(gt(Post::createdOn.name, Date.from(compEndTime.toJavaInstant())))
                        }
                        GameState.ALL -> {}
                    }
                }
                commentRouteLocation?.let {
                    add(eq(Post::route.name, commentRouteLocation))
                }
            }
            try {
                return@withContext collection.find(and(baseFilters))
                    .sort(getSort(sortType))
                    .limit(RETRIEVE_LIM)
                    .map { it.toDomain() }
                    .toList()
            } catch (ex: Exception) {
                logger.error { "Error finding posts by parent id with event filters: $ex" }
                return@withContext emptyList()
            }
        }

    override suspend fun save(posts: List<PostDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = posts.map { post ->
                val entity = post.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext if (bulkOps.isNotEmpty()) collection.bulkWrite(bulkOps).modifiedCount else 0
        }

    override suspend fun saveIfNew(posts: List<PostDomain>) =
        withContext(Dispatchers.IO) {
            val existingPosts = collection.find(
                `in`(ID, posts.map { it.id })
            ).map { it.id }.toSet()
            val bulkOps = posts.mapNotNull { post ->
                if (!existingPosts.contains(post.id)) {
                    val entity = post.toEntity()
                    ReplaceOneModel(
                        eq(ID, entity.id),
                        entity,
                        ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                    )
                } else {
                    null
                }
            }
            val result = if (bulkOps.isNotEmpty()) {
                collection.bulkWrite(bulkOps, BulkWriteOptions().ordered(false)).insertedCount
            } else {
                0
            }

            result
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
                    set(Post::status.name, ActiveStatus.DELETED),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setPostsDeleted(ids: List<String>): Long =
        withContext(Dispatchers.IO) {
            collection.updateMany(
                `in`(ID, ids),
                combine(
                    set(Post::status.name, ActiveStatus.DELETED),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setVotes(
        id: String,
        score: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.find(eq(ID, id)).firstOrNull()?.let { post ->
                val (topScore, hotScore, risingScore) = calculateScores(
                    post.votes + score,
                    post.count,
                    post.createdOn?.toEpochMilliseconds() ?: 0L
                )
                collection.updateOne(
                    eq(ID, id),
                    combine(
                        inc(Post::votes.name, score),
                        set(Post::topScore.name, topScore),
                        set(Post::hotScore.name, hotScore),
                        set(Post::risingScore.name, risingScore),
                        set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                    )
                ).modifiedCount
            } ?: 0L
        }

    override suspend fun setUserRole(
        roleUpdate: RoleUpdate
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                `in`(Post::userId.name, roleUpdate.userId),
                combine(
                    set(Post::userRole.name, roleUpdate.userRole),
                    set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }

    override suspend fun setCount(
        id: String,
        increment: Int // +1 for inc | -1 for dec
    ) =
        withContext(Dispatchers.IO) {
            collection.find(eq(ID, id)).firstOrNull()?.let { post ->
                val (topScore, hotScore, risingScore) = calculateScores(
                    post.votes,
                    post.count + (1 * increment),
                    post.createdOn?.toEpochMilliseconds() ?: 0L
                )
                collection.updateOne(
                    eq(ID, id),
                    combine(
                        inc(Post::count.name, 1 * increment),
                        set(Post::topScore.name, topScore),
                        set(Post::hotScore.name, hotScore),
                        set(Post::risingScore.name, risingScore),
                        set(Post::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                    )
                ).modifiedCount
            } ?: 0L
        }
}

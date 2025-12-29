package com.paraiso.domain.posts

import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.messageTypes.SubscriptionInfo
import com.paraiso.domain.messageTypes.init
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.routes.RouteResponse
import com.paraiso.domain.sport.data.CompetitionResponse
import com.paraiso.domain.sport.data.TeamResponse
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.GAME_PREFIX
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.PLACEHOLDER_ID
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.votes.VotesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

class PostsApi(
    private val postsDB: PostsDB,
    private val votesApi: VotesApi,
    private val followsApi: FollowsApi,
    private val eventService: EventService,
    private val sportApi: SportApi
) {

    // return fully updated root post (for update or load of root post to post tree)
    suspend fun getById(
        postSearchId: PostSearchId
    ): PostsData? =
        postsDB.findById(postSearchId.id)?.let { post ->
            val compStatus = if(post.type == PostType.EVENT && post.id != null){
                sportApi.findCompetitionById(post.id.removePrefix(GAME_PREFIX))?.status
            } else null
            val range = getRange(postSearchId.range, postSearchId.sort)
            val followees = followsApi.getByFollowerId(postSearchId.userId).map { it.followeeId }.toSet()
            val resolvedGameState =
                (postSearchId.gameState
                    ?: when(compStatus?.state){
                        "pre" -> GameState.PRE
                        "in" -> GameState.MID
                        "post" -> GameState.POST
                        else -> null
                    }).takeIf { post.type == PostType.EVENT }
            val subPosts = postsDB.findByParentIdWithEventFilters(
                postSearchId.id,
                range,
                postSearchId.selectedFilters,
                postSearchId.sort,
                followees,
                post.createdOn,
                compStatus?.completedTime,
                resolvedGameState,
                postSearchId.commentRouteLocation.takeIf { post.type == PostType.EVENT }
            )
            generatePostTree(
                post,
                ArrayDeque(subPosts),
                range,
                postSearchId.sort,
                postSearchId.selectedFilters,
                postSearchId.userId,
                followees
            ).let { posts ->
                // pull in event related data - with subscription
                val (teams, comps) = pullEventData(
                    posts, postSearchId.userId, postSearchId.sessionId, subscribe = postSearchId.id == HOME_PREFIX
                )

                PostsData(posts, teams, comps)
            }
        }

    suspend fun getByIdsBasic(userId: String, postSearchIds: Set<String>) =
        postsDB.findByIdsIn(postSearchIds).let { posts ->
            val votes = votesApi.getByUserIdAndPostIdIn(userId, postSearchIds)
            posts.associate { it.id to it.toResponse(votes[it.id]?.upvote) }
        }

    suspend fun getByIds(
        userId: String,
        postSearchIds: Set<String>,
        sessionId: String
    ): PostsData =
        generatePostTree(
            generateBasePost(PLACEHOLDER_ID, PLACEHOLDER_ID),
            ArrayDeque(postsDB.findByIdsIn(postSearchIds)),
            getRange(Range.DAY, SortType.NEW),
            SortType.NEW,
            FilterTypes.init(),
            userId,
            emptySet()
        ).let { posts ->
            // pull in event related data - no subscription
            val (teams, comps) =
                pullEventData(posts, userId, sessionId, subscribe = false)
            posts.remove(PLACEHOLDER_ID)
            PostsData(posts, teams, comps)
        }

    // search by partial for autocomplete
    suspend fun getByPartial(userId: String, search: String, sessionId: String): PostsData =
        postsDB.findByPartial(search).let { foundPosts ->
            val votes = votesApi.getByUserIdAndPostIdIn(userId, foundPosts.mapNotNull { it.id }.toSet())
            val resultPosts = foundPosts.associate { foundPost ->
                (foundPost.id ?: UNKNOWN) to foundPost.toResponse(votes[foundPost.id]?.upvote)
            }
            // pull in event related data - no subscription
            val (teams, comps) = pullEventData(resultPosts, userId, sessionId, subscribe = false)
            PostsData(resultPosts.toMutableMap(), teams, comps)
        }

    suspend fun getPosts(postSearch: PostSearch): PostsData {
        // grab 50 most recent posts at given super level
        val followees = followsApi.getByFollowerId(postSearch.userId).map { it.followeeId }.toSet()
        val range = getRange(postSearch.range, postSearch.sort)
        return postsDB.findByBaseCriteria(
            postSearch.id,
            postSearch.name, // post route
            range,
            postSearch.selectedFilters,
            postSearch.sort,
            followees
        ) // generate base post and post tree off of given inputs
            .let { subPosts ->
                val posts = generatePostTree(
                    generateBasePost(postSearch.id, postSearch.name),
                    ArrayDeque(subPosts),
                    range,
                    postSearch.sort,
                    postSearch.selectedFilters,
                    postSearch.userId,
                    followees
                )
                // pull in event related data
                val (teams, comps) = pullEventData(
                    posts, postSearch.userId, postSearch.sessionId, subscribe = postSearch.id == HOME_PREFIX
                )
                PostsData(posts, teams, comps)
            }
    }

    private suspend fun generatePostTree(
        root: Post,
        postsQueue: ArrayDeque<Post>,
        range: Instant?,
        sortType: SortType,
        filters: FilterTypes,
        userId: String,
        userFollowing: Set<String>
    ) = coroutineScope {
        LinkedHashMap<String, Post>().let { returnPosts ->
            if (root.id != null) returnPosts[root.id] = root
            // add all init sub posts to return
            postsQueue.forEach { post ->
                post.id?.let {
                    returnPosts[post.id] = post
                }
            }
            while (postsQueue.isNotEmpty()) {
                val nextRefNode = postsQueue.removeFirst()
                // no need to search for sub posts if none exist beneath
                val subPosts = if (nextRefNode.count > 0) {
                    postsDB.findByParentId(
                        nextRefNode.id ?: UNKNOWN,
                        range,
                        filters,
                        sortType,
                        userFollowing
                    )
                } else {
                    emptyList()
                }
                subPosts.map { post ->
                    if (post.id != null) {
                        returnPosts[post.id] = post
                        postsQueue.addLast(post)
                    }
                }
            }
            // grab all user votes for each post
            val votes = votesApi.getByUserIdAndPostIdIn(userId, returnPosts.keys)
            val responseMap = LinkedHashMap<String, PostResponse>()
            // map to response object and add user vote response
            returnPosts.reversed().forEach { (id, post) ->
                responseMap[id] = post.toResponse(votes[id]?.upvote)
            }
            responseMap
        }
    }

    private suspend fun pullEventData(
        responseMap: Map<String, PostResponse>,
        userId: String,
        sessionId: String,
        subscribe: Boolean
    ) = coroutineScope {
        val events = responseMap.filter { it.value.type === PostType.EVENT }
        val eventIds = events.map { it.key.removePrefix(GAME_PREFIX) }.toSet()
        if (subscribe) {
            launch {
                subscribeToEvents(eventIds, userId, sessionId)
            }
        }
        val teamResponse = async {
            val teamIds = events.mapNotNull { event ->
                event.value.title?.split("@")?.map { teamAbbr -> "${event.value.data}-${teamAbbr.trim()}" }
            }
            sportApi.findTeamsByIds(teamIds.flatten().toSet()).associateBy { it.id }
        }
        val competitions = sportApi.findCompetitionsByIds(eventIds).associateBy { it.id }
        Pair(teamResponse.await(), competitions)
    }

    private fun getRange(rangeModifier: Range, sortType: SortType) =
        Clock.System.now().let { clock ->
            when (rangeModifier) {
                Range.DAY -> clock.minus(1.days)
                Range.WEEK -> clock.minus(7.days)
                Range.MONTH -> clock.minus(30.days)
                Range.YEAR -> clock.minus(365.days)
                else -> Instant.fromEpochMilliseconds(Long.MIN_VALUE)
            }
        }.takeIf { rangeModifier != Range.ALL && sortType == SortType.TOP }

    suspend fun putPost(message: Message): Unit = coroutineScope {
        message.id?.let { messageId ->
            // if post already exists then edit
            if (message.editId != null) {
                postsDB.editPost(message)
                // otherwise create a new post
            } else {
                postsDB.save(listOf(message.toNewPost()))
                // update parent sub posts
                if (message.replyId != null) {
                    launch {
                        // update relatives sub post counts
                        if (message.rootId != messageId) {
                            postsDB.findById(message.replyId)?.let { parent ->
                                updateCounts(parent, increment = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun votePost(postId: String, score: Int) =
        postsDB.setScore(postId, score)

    suspend fun deletePost(delete: Delete, userId: String) =
        postsDB.findById(delete.postId)?.let { post ->
            if (post.userId == userId) {
                postsDB.setPostDeleted(delete.postId)
                postsDB.findById(delete.parentId)?.let { parent ->
                    updateCounts(parent, increment = -1)
                }
            }
        }

    private suspend fun updateCounts(post: Post, increment: Int) {
        if (post.id != null) {
            postsDB.setCount(post.id, increment)
            if (post.rootId != post.id && post.parentId != null) {
                postsDB.findById(post.parentId)?.let {
                    updateCounts(it, increment)
                }
            }
        }
    }

    // subscribe user to necessary events based on the server/session
    private suspend fun subscribeToEvents(eventIds: Set<String>, userId: String, sessionId: String) = coroutineScope {
        if (eventIds.isNotEmpty()) {
            eventService.getUserSession(userId)?.let { receiveUserSessions ->
                receiveUserSessions.serverSessions.asSequence().find { it.value.contains(sessionId) }
                    ?.let { serverMap ->
                        val subscription = SubscriptionInfo(
                            userId,
                            sessionId,
                            MessageType.COMPS,
                            eventIds,
                            subscribe = true
                        )
                        eventService.publish(
                            "server:${serverMap.key}",
                            "NA:${MessageType.SUBSCRIBE}:${Json.encodeToString(subscription)}"
                        )
                    }
            }
        }
    }
}

@Serializable
data class PostsData(
    val posts: MutableMap<String, PostResponse>,
    val teams: Map<String, TeamResponse>,
    val competitions: Map<String, CompetitionResponse>
)

@Serializable
data class InitRouteData(
    val postsData: PostsData,
    val users: Map<String, UserResponse>,
    val route: RouteResponse
)

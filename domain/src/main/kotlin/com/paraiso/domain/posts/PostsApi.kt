package com.paraiso.domain.posts

import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.init
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.util.Constants.PLACEHOLDER_ID
import com.paraiso.domain.util.Constants.UNKNOWN
import com.paraiso.domain.votes.VotesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class PostsApi(
    private val postsDB: PostsDB,
    private val votesApi: VotesApi,
    private val followsApi: FollowsApi
) {

    // return fully updated root post (for update or load of root post to post tree)
    suspend fun getById(postSearchId: String, rangeModifier: Range, sortType: SortType, filters: FilterTypes, userId: String) =
        postsDB.findById(postSearchId)?.let { post ->
            val range = getRange(rangeModifier, sortType)
            val followees = followsApi.getByFollowerId(userId).map { it.followeeId }.toSet()
            val subPosts = postsDB.findByParentId(postSearchId, range, filters, sortType, followees)
            generatePostTree(
                post,
                ArrayDeque(subPosts),
                range,
                sortType,
                filters,
                userId,
                followees
            )
        }

    suspend fun getByIdsBasic(userId: String, postSearchIds: Set<String>) =
        postsDB.findByIdsIn(postSearchIds).let { posts ->
            val votes = votesApi.getByUserIdAndPostIdIn(userId, postSearchIds)
            posts.associate { it.id to it.toResponse(votes[it.id]?.upvote) }
        }

    suspend fun getByIds(
        userId: String,
        postSearchIds: Set<String>
    ): Map<String, PostResponse> =
        postsDB.findByIdsIn(postSearchIds).let { subPosts ->
            generatePostTree(
                generateBasePost(PLACEHOLDER_ID, PLACEHOLDER_ID),
                ArrayDeque(subPosts),
                getRange(Range.DAY, SortType.NEW),
                SortType.NEW,
                FilterTypes.init(),
                userId,
                emptySet()
            ) - PLACEHOLDER_ID // remove unnecessary base post
        }

    // search by partial for autocomplete
    suspend fun getByPartial(userId: String, search: String) =
        postsDB.findByPartial(search).let { foundPosts ->
            val votes = votesApi.getByUserIdAndPostIdIn(userId, foundPosts.mapNotNull { it.id }.toSet())
            foundPosts.map { foundPost ->
                foundPost.toResponse(votes[foundPost.id]?.upvote)
            }
        }

    suspend fun getPosts(
        postSearchId: String,
        basePostName: String,
        rangeModifier: Range,
        sortType: SortType,
        filters: FilterTypes,
        userId: String
    ): LinkedHashMap<String, PostResponse> {
        // grab 50 most recent posts at given super level
        val followees = followsApi.getByFollowerId(userId).map { it.followeeId }.toSet()
        val range = getRange(rangeModifier, sortType)
        return postsDB.findByBaseCriteria(
            postSearchId,
            basePostName, // post route
            range,
            filters,
            sortType,
            followees
        ) // generate base post and post tree off of given inputs
            .let { subPosts ->
                generatePostTree(
                    generateBasePost(postSearchId, basePostName),
                    ArrayDeque(subPosts),
                    range,
                    sortType,
                    filters,
                    userId,
                    followees
                )
            }
    }

    private suspend fun generatePostTree(
        root: Post,
        postsQueue: ArrayDeque<Post>,
        range: Instant,
        sortType: SortType,
        filters: FilterTypes,
        userId: String,
        userFollowing: Set<String>
    ) =
        LinkedHashMap<String, Post>().let { returnPosts ->
            if (root.id != null) returnPosts[root.id] = root
            //add all init sub posts to return
            postsQueue.forEach {post ->
                post.id?.let{
                    returnPosts[post.id] = post
                }
            }
            while (postsQueue.isNotEmpty()) {
                val nextRefNode = postsQueue.removeFirst()
                //no need to search for sub posts if none exist beneath
                val subPosts = if(nextRefNode.count > 0 ) {
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
            //grab all user votes for each post
            val votes = votesApi.getByUserIdAndPostIdIn(userId, returnPosts.keys)
            val responseMap = LinkedHashMap<String, PostResponse>()
            //map to response object and add user vote response
            returnPosts.reversed().forEach  { (id, post) ->
                responseMap[id] = post.toResponse(votes[id]?.upvote)
            }
            responseMap
        }

    private fun getRange(rangeModifier: Range, sortType: SortType) =
        Instant.fromEpochMilliseconds(Long.MIN_VALUE) // ignore range if looking not finding top posts or range all
            .takeIf { rangeModifier == Range.ALL || sortType != SortType.TOP }
            ?: Clock.System.now().let { clock ->
                when (rangeModifier) {
                    Range.DAY -> clock.minus(1.days)
                    Range.WEEK -> clock.minus(7.days)
                    Range.MONTH -> clock.minus(30.days)
                    Range.YEAR -> clock.minus(365.days)
                    else -> Instant.fromEpochMilliseconds(Long.MIN_VALUE)
                }
            }

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
                        if (message.rootId != message.id) {
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

}

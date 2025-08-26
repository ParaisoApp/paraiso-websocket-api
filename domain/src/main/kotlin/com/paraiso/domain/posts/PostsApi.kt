package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.users.UsersDB
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class PostsApi(
    private val postsDB: PostsDB,
    private val usersDB: UsersDB
) {

    // return fully updated root post (for update or load of root post to post tree)
    suspend fun getById(postSearchId: String, rangeModifier: Range, sortType: SortType, filters: FilterTypes, userId: String) =
        postsDB.findById(postSearchId)?.let { post ->
            val userFollowing = usersDB.getFollowingById(userId).map { it.id }.toSet()
            generatePostTree(
                post, getRange(rangeModifier, sortType), sortType, filters, userFollowing
            )
        }

    suspend fun getByIdBasic(postSearchId: String) =
        postsDB.findById(postSearchId)?.toResponse()

    suspend fun getByIdsBasic(
        postSearchIds: Set<String>
    ): Map<String, PostResponse> = postsDB.findByIdsIn(postSearchIds).associate { (it.id ?: UNKNOWN) to it.toResponse() }

    // search by partial for autocomplete
    suspend fun getByPartial(search: String) =
        postsDB.findByPartial(search).let { foundPosts ->
            foundPosts.map { foundPost ->
                foundPost.toResponse()
            }
        }

    suspend fun getPosts(
        postSearchId: String,
        basePostName: String,
        rangeModifier: Range,
        sortType: SortType,
        filters: FilterTypes,
        userId: String
    ): LinkedHashMap<String, PostResponse>  {
        // grab 50 most recent posts at given super level
        val userFollowing = usersDB.getFollowingById(userId).map { it.id }.toSet()
        val range = getRange(rangeModifier, sortType)
        return postsDB.findByBaseCriteria(
            postSearchId, range, filters, sortType, userFollowing
        ).mapNotNull { it.id }.toSet() // generate base post and post tree off of given inputs
            .let { subPosts ->
                generatePostTree(
                    generateBasePost(postSearchId, basePostName, subPosts),
                    range, sortType, filters, userFollowing
                )
            }
    }

    private suspend fun generatePostTree(
        basePost: Post,
        range: Instant,
        sortType: SortType,
        filters: FilterTypes,
        userFollowing: Set<String>
    ) =
        LinkedHashMap<String, PostResponse>().let { returnPosts ->
            basePost.toResponse().let { root -> // build tree with bfs
                if (root.id != null) returnPosts[root.id] = root
                val refQueue = ArrayDeque(listOf(basePost))
                while (refQueue.isNotEmpty()) {
                    val nextRefNode = refQueue.removeFirst()
                    postsDB.findBySubpostIds(
                        nextRefNode.subPosts, range, filters, sortType, userFollowing
                    ).map { post ->
                        if (post.id != null) {
                            returnPosts[post.id] = post.toResponse()
                            refQueue.addLast(post)
                        }
                    }
                }
            }
            returnPosts
        }

    private fun getRange(rangeModifier: Range, sortType: SortType) =
        Instant.fromEpochMilliseconds(Long.MIN_VALUE) // ignore range if looking at new posts or range is set to all
            .takeIf { rangeModifier == Range.ALL || sortType == SortType.NEW }
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
            if(message.editId != null){
                postsDB.editPost(message)
                // otherwise create a new post
            } else {
                postsDB.save(listOf(message.toNewPost()))
                // update parent sub posts
                if(message.replyId != null) {
                    postsDB.addSubpostToParent(message.replyId, messageId)
                    launch {
                        // update relatives sub post counts
                        if (message.rootId != message.id) {
                            postsDB.findById(message.replyId)?.let {parent ->
                                updateCounts(parent, increment = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun votePost(vote: Vote) =
        postsDB.findById(vote.postId)?.votes?.let { votes ->
            if (votes.containsKey(vote.voterId) && votes[vote.voterId] == vote.upvote) {
                postsDB.removeVotes(vote.postId, vote.voterId)
            } else {
                postsDB.addVotes(vote.postId, vote.voterId, vote.upvote)
            }
        }

    suspend fun deletePost(delete: Delete, userId: String) =
        postsDB.findById(delete.postId)?.let {post ->
            if (post.userId == userId) {
                postsDB.setPostDeleted(delete.postId)
                postsDB.findById(delete.parentId)?.let{parent ->
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

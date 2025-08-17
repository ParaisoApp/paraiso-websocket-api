package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Delete
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.USER_PREFIX
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class PostsApi {

    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 5
        const val TIME_WEIGHTING = 10000000000
    }

    // return fully updated root post (for update or load of root post to post tree)
    fun getById(postSearchId: String, rangeModifier: Range, sortType: SortType, filters: FilterTypes, userId: String) =
        ServerState.posts[postSearchId]?.let { post ->
            generatePostTree(
                post,
                postSearchId,
                getRange(rangeModifier, sortType),
                sortType,
                filters,
                userId
            )
        }

    // search by partial for autocomplete
    fun getByPartial(search: String) =
        ServerState.posts.values.filter {
            it.title?.lowercase()?.contains(search.lowercase()) == true || it.content?.lowercase()?.contains(search.lowercase()) == true
        }.take(PARTIAL_RETRIEVE_LIM).let { foundPosts ->
            foundPosts.map { foundPost ->
                foundPost.toResponse()
            }
        }

    fun getPosts(
        postSearchId: String,
        basePostName: String,
        rangeModifier: Range,
        sortType: SortType,
        filters: FilterTypes,
        userId: String
    ) =
        // grab 100 most recent posts at given super level
        getRange(rangeModifier, sortType).let { range ->
            val userFollowing = ServerState.userList[userId]?.following ?: setOf()
            ServerState.posts.asSequence().filter { (_, post) ->
                ( // check for base post or user if profile nav
                        post.parentId == postSearchId ||
                                post.userId == postSearchId.removePrefix(USER_PREFIX) ||
                                (postSearchId == SiteRoute.HOME.name && post.rootId == post.id)
                        ) &&
                        post.createdOn != null &&
                        post.createdOn > range &&
                        post.status != PostStatus.DELETED &&
                        filters.postTypes.contains(post.type) &&
                        (
                                filters.userRoles.contains(ServerState.userList[post.userId]?.roles) ||
                                        (filters.userRoles.contains(UserRole.FOLLOWING) && userFollowing.contains(post.userId))
                                )
            }.sortedBy { getSort(it, sortType) } // get and apply sort by
                .take(RETRIEVE_LIM)
                .map { it.key }.toSet() // generate base post and post tree off of given inputs
                .let { subPosts ->
                    generatePostTree(
                        generateBasePost(postSearchId, basePostName, subPosts),
                        postSearchId,
                        range,
                        sortType,
                        filters,
                        userId
                    )
                }
        }

    private fun generatePostTree(
        basePost: Post,
        postSearchId: String,
        range: Instant,
        sortType: SortType,
        filters: FilterTypes,
        userId: String
    ) =
        LinkedHashMap<String, PostResponse>().let { returnPosts ->
            basePost.toResponse().let { root -> // build tree with bfs
                val userFollowing = ServerState.userList[userId]?.following ?: setOf()
                if (root.id != null) returnPosts[root.id] = root
                val refQueue = ArrayDeque(listOf(basePost))
                ServerState.posts
                    .filter { (_, gamePost) ->
                        gamePost.type == PostType.GAME &&
                                (gamePost.parentId?.lowercase() == root.id?.lowercase() || gamePost.data == postSearchId)
                    }.forEach { (_, gamePost) ->
                        if (gamePost.id != null) {
                            returnPosts[gamePost.id] = gamePost.toResponse()
                            refQueue.addLast(gamePost)
                        }
                    }
                while (refQueue.isNotEmpty()) {
                    val nextRefNode = refQueue.removeFirst()
                    ServerState.posts
                        .filterKeys { it in nextRefNode.subPosts }
                        .asSequence()
                        .filter { (_, post) ->
                            post.createdOn != null &&
                                    post.createdOn > range &&
                                    post.status != PostStatus.DELETED &&
                                    filters.postTypes.contains(post.type) &&
                                    (
                                            filters.userRoles.contains(ServerState.userList[post.userId]?.roles) ||
                                                    (filters.userRoles.contains(UserRole.FOLLOWING) && userFollowing.contains(post.userId))
                                            )
                        }.sortedBy { getSort(it, sortType) }
                        .take(RETRIEVE_LIM)
                        .associateTo(LinkedHashMap()) { (id, post) ->
                            (
                                    id to post.toResponse()
                                    ).also { (_, returnPost) ->
                                    if (returnPost.id != null) {
                                        returnPosts[returnPost.id] = returnPost
                                        refQueue.addLast(post)
                                    }
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

    private fun getSort(entry: Map.Entry<String, Post>, sortType: SortType) =
        entry.value.createdOn?.toEpochMilliseconds()?.let { created ->
            if (sortType == SortType.NEW) {
                created
            } else {
                // sortedByAscending so take inverse
                entry.value.votes.values.sumOf { bool -> 1.takeIf { bool } ?: -1 }.toLong().let { votes ->
                    if (sortType == SortType.HOT) {
                        (created / TIME_WEIGHTING) * votes
                    } else {
                        votes
                    }
                }
            }
        }


    suspend fun putPost(message: Message) = coroutineScope {
        message.id?.let { messageId ->
            val now = Clock.System.now()
            // if post already exists then edit
            ServerState.posts[messageId]?.let { existingPost ->
                ServerState.posts[messageId] = existingPost.copy(
                    title = message.title,
                    content = message.content,
                    media = message.media,
                    data = message.data,
                    updatedOn = now
                )
                // otherwise create a new post
            } ?: run {
                ServerState.posts[messageId] = message.toNewPost()
                // update parent sub posts
                ServerState.posts[message.replyId]?.let { parent ->
                    if (message.replyId != null) {
                        ServerState.posts[message.replyId] = parent.copy(
                            count = parent.count + 1,
                            subPosts = parent.subPosts + messageId,
                            updatedOn = now
                        )
                    }
                    launch {
                        // update grandparent sub post counts - increment to add
                        ServerState.posts[parent.parentId]?.let { grandParent ->
                            updateCounts(grandParent, now, increment = 1)
                        }
                    }
                }
            }
        }
    }

    fun votePost(vote: Vote) =
        ServerState.posts[vote.postId]?.let { post ->
            post.votes.toMutableMap().let { mutableVoteMap ->
                if (mutableVoteMap.containsKey(vote.voterId) && mutableVoteMap[vote.voterId] == vote.upvote) {
                    mutableVoteMap.remove(vote.voterId)
                } else {
                    mutableVoteMap[vote.voterId] = vote.upvote
                }
                ServerState.posts[vote.postId] =
                    post.copy(votes = mutableVoteMap.toMap(), updatedOn = Clock.System.now())
            }
        }

    fun deletePost(delete: Delete, userId: String) =
        ServerState.posts[delete.postId]?.let { post ->
            if (post.userId == userId) {
                Clock.System.now().let { now ->
                    ServerState.posts[delete.postId] =
                        post.copy(status = PostStatus.DELETED, updatedOn = now)
                    // update parent sub post counts - decrement to subtract
                    ServerState.posts[post.parentId]?.let { parent ->
                        updateCounts(parent, now, increment = -1)
                    }
                }
            }
        }

    private fun updateCounts(post: Post, now: Instant, increment: Int) {
        if (post.id != null) {
            ServerState.posts[post.id] = post.copy(
                count = post.count + (1 * increment),
                updatedOn = now
            )
        }
        if (post.rootId == post.id) return
        ServerState.posts[post.parentId]?.let { parent ->
            updateCounts(parent, now, increment)
        }
    }
}

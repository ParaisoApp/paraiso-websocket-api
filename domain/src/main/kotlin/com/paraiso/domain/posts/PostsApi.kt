package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.SiteRoute
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.users.UserReturn
import com.paraiso.domain.users.buildUser
import com.paraiso.domain.users.systemUser
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class PostsApi {

    companion object {
        const val RETRIEVE_LIM = 50
        const val TIME_WEIGHTING = 10000000000
    }

    fun putPost(message: Message) {
        message.id?.let { messageId ->
            val now = Clock.System.now()
            // if post already exists then edit
            ServerState.posts[messageId]?.let { existingPost ->
                ServerState.posts[messageId] = existingPost.copy(
                    title = message.title,
                    content = message.content,
                    media = message.media,
                    updatedOn = now
                )
                // otherwise create a new post
            } ?: run {
                ServerState.posts[messageId] = message.toNewPost()
                // update parent sub posts
                ServerState.posts[message.replyId]?.let { parent ->
                    ServerState.posts[message.replyId] = parent.copy(
                        subPosts = parent.subPosts + messageId,
                        updatedOn = now
                    )
                }
                // update user posts
                ServerState.userList[message.userId]?.let { user ->
                    ServerState.userList[message.userId] = user.copy(
                        posts = user.posts + messageId,
                        updatedOn = now
                    )
                }
                // update user post replies
                if (message.userId != message.userReceiveId) {
                    ServerState.userList[message.userReceiveId]?.let { user ->
                        ServerState.userList[message.userReceiveId] = user.copy(
                            replies = user.replies + messageId,
                            updatedOn = now
                        )
                    }
                }
            }
        }
    }

    //return fully updated root post (for update or load of root post to post tree)
    fun getPostById(postSearchId: String, rangeModifier: Range, sortType: SortType, filters: FilterTypes) =
        ServerState.posts[postSearchId]?.let{ post ->
            generatePostTree(
                post,
                ServerState.userList[post.userId]?.let { user ->
                    buildUser(user)
                } ?: UserReturn.systemUser(),
                postSearchId,
                getRange(rangeModifier, sortType),
                sortType,
                filters
            )
        }


    fun getPosts(postSearchId: String, basePostName: String, rangeModifier: Range, sortType: SortType, filters: FilterTypes) =
        // grab 100 most recent posts at given super level
        getRange(rangeModifier, sortType).let { range ->
            ServerState.posts.asSequence().filter {(_, post) ->
                ( // check for base post or user if profile nav
                    post.parentId == postSearchId ||
                    post.userId == postSearchId.removePrefix("USER-") ||
                        (
                            postSearchId == SiteRoute.HOME.name && // search from all posts if on homepage
                            enumValues<SiteRoute>().any{ it.name == post.parentId }
                        )
                ) &&
                    post.createdOn > range &&
                    filters.postTypes.contains(post.type) &&
                    filters.userRoles.contains(ServerState.userList[post.userId]?.roles)
            }.sortedBy { getSort(it, sortType) } // get and apply sort by
                .take(RETRIEVE_LIM)
                .map { it.key }.toSet() // generate base post and post tree off of given inputs
                .let { subPosts ->
                    generatePostTree(
                        generateBasePost(postSearchId, basePostName, subPosts),
                        UserReturn.systemUser(),
                        postSearchId,
                        range,
                        sortType,
                        filters
                    )
                }
        }

    private fun generatePostTree(
        basePost: Post,
        baseUser: UserReturn,
        postSearchId: String,
        range: Instant,
        sortType: SortType,
        filters: FilterTypes
    ) =
        basePost.toPostReturn(baseUser).let { root -> // build tree with bfs
            val refQueue = ArrayDeque(listOf(basePost))
            val returnQueue = ArrayDeque(listOf(root))
            while (refQueue.isNotEmpty()) {
                val nextRefNode = refQueue.removeFirst()
                val nextReturnNode = returnQueue.removeFirst()
                val children = ServerState.posts
                    .filterKeys { it in nextRefNode.subPosts }
                    .asSequence()
                    .filter { (_, post) ->
                        post.createdOn > range  &&
                        filters.postTypes.contains(post.type) &&
                        filters.userRoles.contains(ServerState.userList[post.userId]?.roles)
                    }.sortedBy { getSort(it, sortType) }
                    .take(RETRIEVE_LIM)
                    .associateTo( LinkedHashMap() ) { (id, post) ->
                        (
                            id to post.toPostReturn(
                                ServerState.userList[post.userId]?.let { user ->
                                    buildUser(user)
                                }
                            )
                            ).also { (_, returnPost) ->
                            returnQueue.addLast(returnPost)
                            refQueue.addLast(post)
                        }
                    }
                nextReturnNode.subPosts = children
            }
            root.also { rootRef -> // generate relevant sport posts
                rootRef.subPosts = rootRef.subPosts.plus(
                    ServerState.sportPosts
                        .filter { (_, post) -> post.parentId == root.id || post.data == postSearchId }
                        .mapValues {
                            it.value.toPostReturn(
                                ServerState.userList[it.value.userId]?.let { user ->
                                    buildUser(user)
                                }
                            )
                        }
                )
            }
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
        entry.value.createdOn.toEpochMilliseconds().let { created ->
            if (sortType == SortType.NEW) {
                created
            } else {
                //sortedByAscending so take inverse
                entry.value.votes.values.sumOf { bool -> 1.takeIf { bool } ?: -1 }.toLong().let { votes ->
                    if (sortType == SortType.HOT) {
                        (created / TIME_WEIGHTING) * votes
                    } else {
                        votes
                    }
                }
            }
        }

    fun votePost(vote: Vote) {
        ServerState.posts[vote.postId]?.let { post ->
            post.votes.toMutableMap().let { mutableVoteMap ->
                if (mutableVoteMap.containsKey(vote.userId) && mutableVoteMap[vote.userId] == vote.upvote) {
                    mutableVoteMap.remove(vote.userId)
                } else {
                    mutableVoteMap[vote.userId] = vote.upvote
                }
                ServerState.posts[vote.postId] =
                    post.copy(votes = mutableVoteMap.toMap(), updatedOn = Clock.System.now())
            }
        }
    }
}

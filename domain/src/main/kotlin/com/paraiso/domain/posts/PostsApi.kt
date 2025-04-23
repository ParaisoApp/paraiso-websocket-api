package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
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
            // if post already exists then edit
            ServerState.posts[messageId]?.let { existingPost ->
                ServerState.posts[messageId] = existingPost.copy(
                    title = message.title,
                    content = message.content,
                    media = message.media,
                    updatedOn = Clock.System.now()
                )
                // otherwise create a new post
            } ?: run {
                ServerState.posts[messageId] = message.toNewPost()
                // update parent sub posts
                ServerState.posts[message.replyId]?.let { parent ->
                    ServerState.posts[message.replyId] = parent.copy(
                        subPosts = parent.subPosts.plus(messageId),
                        updatedOn = Clock.System.now()
                    )
                }
                // update user posts
                ServerState.userList[message.userId]?.let { user ->
                    ServerState.userList[message.userId] = user.copy(
                        posts = user.posts.plus(messageId),
                        updatedOn = Clock.System.now()
                    )
                }
            }
        }
    }

    fun getPosts(basePostId: String, basePostName: String, rangeModifier: Range, sortType: SortType) =
        // grab 100 most recent posts at given super level
        getRange(rangeModifier, sortType).let{range ->
            ServerState.posts.filter { it.value.parentId == basePostId && it.value.createdOn > range }
                .entries.take(RETRIEVE_LIM).sortedBy { getSort(it, sortType) } // get sort by
                .map { it.key }.toSet()// generate base post and post tree off of given inputs
                .let { subPosts -> generatePostTree(basePostId, basePostName, subPosts, range, sortType) }
        }

    private fun generatePostTree(
        basePostId: String,
        basePostName: String,
        subPosts: Set<String>,
        range: Instant,
        sortType: SortType
    ) =
        generateBasePost(basePostId, basePostName, subPosts).let { basePost ->
            basePost.toPostReturn().let { root -> // build tree with bfs
                val refQueue = ArrayDeque(listOf(basePost))
                val returnQueue = ArrayDeque(listOf(root))
                while (refQueue.isNotEmpty()) {
                    val nextRefNode = refQueue.removeFirst()
                    val nextReturnNode = returnQueue.removeFirst()
                    val children = ServerState.posts
                        .filterKeys { nextRefNode.subPosts.contains(it) }
                        .asSequence()
                        .filter { it.value.createdOn > range }
                        .toList().take(RETRIEVE_LIM)
                        .sortedBy { getSort(it, sortType) }
                        .associate { (key, value) ->
                            (key to value.toPostReturn()).also { (_, returnNode) ->
                                returnQueue.addLast(returnNode)
                                refQueue.addLast(value)
                            }
                        }
                    nextReturnNode.subPosts = children
                }
                root.also { rootRef -> // generate relevant sport posts
                    rootRef.subPosts = rootRef.subPosts.plus(
                        ServerState.sportPosts.entries.filter { entry ->
                            entry.value.parentId == root.id || entry.value.data == basePostId
                        }.associate { it.key to it.value.toPostReturn() }
                    )
                }
            }
        }

    private fun getRange(rangeModifier: Range, sortType: SortType) =
        Instant.fromEpochMilliseconds(Long.MIN_VALUE)
            .takeIf { rangeModifier == Range.ALL || sortType == SortType.NEW } ?:
        Clock.System.now().let{clock ->
            when(rangeModifier){
                Range.DAY -> clock.minus(1.days)
                Range.WEEK -> clock.minus(7.days)
                Range.MONTH -> clock.minus(30.days)
                Range.YEAR -> clock.minus(365.days)
                else -> Instant.fromEpochMilliseconds(Long.MIN_VALUE)
            }
        }

    private fun getSort(entry: Map.Entry<String, Post>, sortType: SortType) =
        entry.value.createdOn.toEpochMilliseconds().let{created ->
            if(sortType == SortType.NEW){
                created
            }else{
                entry.value.votes.values.filter { it }.size.toLong().let{votes ->
                    if(sortType == SortType.HOT) {
                        (created / TIME_WEIGHTING) * votes
                    }else{
                        votes
                    }
                }
            }
        }

    fun votePost(vote: Vote) {
        ServerState.posts[vote.postId]?.let { post ->
            post.votes.toMutableMap().let { mutableVoteMap ->
                if (mutableVoteMap.containsKey(vote.userId)) {
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

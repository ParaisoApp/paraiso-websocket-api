package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock

class PostsApi {

    companion object {
        const val RETRIEVE_LIM = 100
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

    fun getPosts(basePostId: String, basePostName: String) =
        // grab 100 most recent posts at given super level
        ServerState.posts.filter { it.value.parentId == basePostId }
            .entries.take(RETRIEVE_LIM).sortedBy { it.value.createdOn }
            .map { it.key }.toSet()
            .let { subPosts ->
                generateBasePost(basePostId, basePostName, subPosts).let { basePost ->
                    basePost.toPostReturn().let { root ->
                        val refQueue = ArrayDeque(listOf(basePost))
                        val returnQueue = ArrayDeque(listOf(root))
                        while (refQueue.isNotEmpty()) {
                            val nextRefNode = refQueue.removeFirst()
                            val nextReturnNode = returnQueue.removeFirst()
                            val children = ServerState.posts
                                .filterKeys { nextRefNode.subPosts.contains(it) }
                                .toList().take(RETRIEVE_LIM)
                                .sortedBy { it.second.createdOn }
                                .associate { (key, value) ->
                                    (key to value.toPostReturn()).also { (_, returnNode) ->
                                        returnQueue.addLast(returnNode)
                                        refQueue.addLast(value)
                                    }
                                }
                            nextReturnNode.subPosts = children
                        }
                        root.also { rootRef ->
                            println(ServerState.sportPosts)
                            rootRef.subPosts = rootRef.subPosts.plus(
                                ServerState.sportPosts.entries.filter { entry ->
                                    entry.value.parentId == root.id || entry.value.data == basePostId
                                }.associate { it.key to it.value.toPostReturn() }
                            )
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

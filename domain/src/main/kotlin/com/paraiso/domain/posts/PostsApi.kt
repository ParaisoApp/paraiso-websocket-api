package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock

class PostsApi {
    fun putPost(message: Message){
        message.id?.let{messageId ->
            // if post already exists then edit
            ServerState.posts[messageId]?.let{existingPost ->
                ServerState.posts[messageId] = existingPost.copy(
                    title = message.title,
                    content = message.content,
                    media = message.media,
                    updatedOn = Clock.System.now()
                )
                //otherwise create a new post
            } ?: run {
                ServerState.posts[messageId] = message.toNewPost()
                //update parent sub posts
                ServerState.posts[message.replyId]?.let{parent ->
                    ServerState.posts[message.replyId] = parent.copy(
                        subPosts = parent.subPosts.plus(messageId),
                        updatedOn = Clock.System.now()
                    )
                }
                //update user posts
                ServerState.userList[message.userId]?.let{user ->
                    ServerState.userList[message.userId] = user.copy(
                        posts = user.posts.plus(messageId),
                        updatedOn = Clock.System.now()
                    )
                }
            }
        }
    }

    fun getPosts() =
        ServerState.posts[ServerState.basePost.id]?.let{ basePost ->
            basePost.toPostReturn().let { root ->
                val refQueue = ArrayDeque(listOf(basePost))
                val returnQueue = ArrayDeque(listOf(root))
                while(refQueue.isNotEmpty()){
                    val nextRefNode = refQueue.removeFirst()
                    val nextReturnNode = returnQueue.removeFirst()
                    val children = ServerState.posts
                        .filterKeys { nextRefNode.subPosts.contains(it) }
                        .toList()
                        .sortedBy { it.second.createdOn }
                        .take(100).associate { (key, value) ->
                            (key to value.toPostReturn()).also { (_, returnNode) ->
                                returnQueue.addLast(returnNode)
                                refQueue.addLast(value)
                            }
                        }
                    nextReturnNode.subPosts = children
                }
                root.also {rootRef ->
                    rootRef.subPosts = rootRef.subPosts.plus(
                        ServerState.sportPosts.entries.filter { entry ->
                            entry.value.parentId == root.id
                        }.associate { it.key to it.value.toPostReturn() }
                    )
                }
            }
        } ?: run { ServerState.basePost.toPostReturn() }


    fun votePost(vote: Vote) {
        ServerState.posts[vote.postId]?.let{ post ->
            post.votes.toMutableMap().let { mutableVoteMap ->
                if(mutableVoteMap.containsKey(vote.userId)){
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
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
            }
        }
    }

    fun getPosts(): PostReturn {
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
                return root
            }
        }
        return ServerState.basePost.toPostReturn()
    }

    fun votePost(vote: Vote) {
        ServerState.posts[vote.postId]?.let{ post ->
            val (newUpvoted, newDownvoted) = if(vote.upvote){
                val upvoted = if(post.upvoted.containsKey(vote.userId)){
                    post.upvoted - vote.userId
                } else {
                    post.upvoted + (vote.userId to true)
                }
                Pair(upvoted, post.downvoted - vote.userId)
            }else{
                val downvoted = if(post.downvoted.containsKey(vote.userId)){
                    post.downvoted - vote.userId
                } else {
                    post.downvoted + (vote.userId to true)
                }
                Pair(post.upvoted - vote.userId, downvoted)
            }
            ServerState.posts[vote.postId] =
                post.copy(upvoted = newUpvoted, downvoted = newDownvoted, updatedOn = Clock.System.now())
        }
    }
}
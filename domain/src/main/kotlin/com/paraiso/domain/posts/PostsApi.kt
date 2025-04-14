package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.messageTypes.toNewPost
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock

class PostsApi {
    fun putPost(message: Message){
        message.id?.let{messageId ->
            // if post already exists then we edit
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
                ServerState.posts[message.replyId]?.let{existingParent ->
                    ServerState.posts[message.replyId] = existingParent.copy(
                        subPosts = existingParent.subPosts.plus(messageId)
                    )
                }
            }
        }
    }

    fun getPosts(): TreeNode{
        TreeNode(value = ServerState.basePost).let { root ->
            val queue = ArrayDeque(listOf(root))
            while(queue.isNotEmpty()){
                val nextNode = queue.removeFirst()
                ServerState.posts.entries
                    .filter{nextNode.value.subPosts.contains(it.key)}
                    .sortedBy{it.value.createdOn}
                    .take(100)
                    .forEach{(key, value) ->
                        TreeNode(value).let { newNode ->
                            queue.addLast(newNode)
                            nextNode.children[key] = newNode
                        }
                    }

            }
            return root
        }
    }

    fun votePost(vote: Vote) {
        ServerState.posts[vote.postId]?.let{post ->
            ServerState.posts[vote.postId] =
                post.copy(upVoted = post.upVoted.plus(vote.userId), updatedOn = Clock.System.now())
                    .takeIf { vote.upvote } ?:
                    post.copy(upVoted = post.downVoted.plus(vote.userId), updatedOn = Clock.System.now())
        }
    }
}
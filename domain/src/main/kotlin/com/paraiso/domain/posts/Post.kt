package com.paraiso.domain.posts

import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.datetime.Instant

data class Post(
    val id: String?,
    val userId: String,
    val title: String,
    val content: String,
    val media: String?,
    val upVoted: Set<String>,
    val downVoted: Set<String>,
    val parentId: String,
    val status: PostStatus,
    val data: String?,
    val subPosts: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

data class TreeNode(
    val value: Post,
    val children: MutableMap<String, TreeNode> = mutableMapOf()
)

enum class PostStatus {
    ACTIVE,
    DELETED,
    ARCHIVED
}

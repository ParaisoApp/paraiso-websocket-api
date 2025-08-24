package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.users.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class PostSearchInfo(
    val postSearchIds: Set<String>,
    val postFilters: FilterTypes
)

@Serializable
data class Ban(
    val userId: String
)

@Serializable
data class Block(
    val userId: String
)
@Serializable
data class Delete(
    val postId: String,
    val parentId: String
)
@Serializable
data class FilterTypes(
    val postTypes: Set<PostType>,
    val userRoles: Set<UserRole>
)

@Serializable
data class Follow(
    val followerId: String,
    val followeeId: String
)

@Serializable
data class Login(
    val userId: String,
    val email: String,
    val password: String
)
@Serializable
data class Report(
    val id: String // can be post or userId
)
@Serializable
data class Tag(
    val userId: String,
    val tag: String
)

@Serializable
data class Vote(
    val voterId: String,
    val voteeId: String,
    val type: PostType,
    val postId: String,
    val upvote: Boolean
)

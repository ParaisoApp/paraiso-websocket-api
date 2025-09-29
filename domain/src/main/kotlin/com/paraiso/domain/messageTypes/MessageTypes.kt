package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
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
){ companion object }

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
data class VoteResponse(
    val id: String? = null,
    val voterId: String,
    val voteeId: String? = null,
    val type: PostType,
    val postId: String,
    val upvote: Boolean,
    val score: Int? = 0,
    val createdOn: Instant? = Clock.System.now(),
    val updatedOn: Instant? = Clock.System.now()
)

@Serializable
data class Vote(
    @SerialName(ID) val id: String? = null,
    val voterId: String,
    val voteeId: String? = null,
    val type: PostType,
    val postId: String,
    val upvote: Boolean,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

fun VoteResponse.toDomain() = Vote(
    id = id ?: "$voterId-$postId",
    voterId = voterId,
    voteeId = voteeId,
    type = type,
    postId = postId,
    upvote = upvote,
    createdOn = createdOn ?: Clock.System.now(),
    updatedOn = updatedOn ?: Clock.System.now(),
)

fun Vote.toResponse() = VoteResponse(
    id = id,
    voterId = voterId,
    voteeId = voteeId,
    type = type,
    postId = postId,
    upvote = upvote,
    createdOn = createdOn,
    updatedOn = updatedOn,
)

fun FilterTypes.Companion.init() = FilterTypes(
    postTypes = setOf(
        PostType.COMMENT,
        PostType.SUB,
        PostType.EVENT
    ),
    userRoles = setOf(
        UserRole.FOLLOWING,
        UserRole.USER,
        UserRole.GUEST
    )
)

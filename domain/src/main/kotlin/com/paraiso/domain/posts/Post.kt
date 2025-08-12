package com.paraiso.domain.posts

import com.paraiso.domain.util.Constants
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Post(
    val id: String?,
    val userId: String?,
    val title: String?,
    val content: String?,
    val type: PostType,
    val votes: Map<String, Boolean>,
    val parentId: String?,
    val rootId: String?,
    val status: PostStatus,
    val media: String?,
    val data: String?,
    val subPosts: Set<String>,
    val count: Int,
    val route: String,
    val createdOn: Instant?,
    val updatedOn: Instant?
) { companion object }

@Serializable
data class PostReturn(
    val id: String?,
    val userId: String?,
    val title: String?,
    val content: String?,
    val type: PostType,
    val votes: Map<String, Boolean>,
    val parentId: String?,
    val rootId: String?,
    val status: PostStatus,
    val media: String?,
    val data: String?,
    var subPosts: Map<String, Boolean>,
    val count: Int,
    val route: String,
    val createdOn: Instant?,
    val updatedOn: Instant?
) { companion object }

@Serializable
enum class PostType {
    @SerialName("SUPER")
    SUPER,

    @SerialName("SUB")
    SUB,

    @SerialName("PROFILE")
    PROFILE,

    @SerialName("COMMENT")
    COMMENT,

    @SerialName("GAME")
    GAME
}

@Serializable
enum class PostStatus {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("DELETED")
    DELETED,

    @SerialName("ARCHIVED")
    ARCHIVED
}

@Serializable
enum class SortType {
    @SerialName("NEW")
    NEW,

    @SerialName("HOT")
    HOT,

    @SerialName("TOP")
    TOP
}

@Serializable
enum class FilterType {
    @SerialName("POSTS")
    POSTS,

    @SerialName("COMMENTS")
    COMMENTS,

    @SerialName("USERS")
    USERS,

    @SerialName("GUESTS")
    GUESTS
}

@Serializable
enum class Range {
    @SerialName("DAY")
    DAY,

    @SerialName("WEEK")
    WEEK,

    @SerialName("MONTH")
    MONTH,

    @SerialName("YEAR")
    YEAR,

    @SerialName("ALL")
    ALL
}

fun Post.toPostReturn() = PostReturn(
    id = id,
    userId = userId,
    title = title,
    content = content,
    type = type,
    media = media,
    votes = votes,
    parentId = parentId,
    rootId = rootId,
    status = status,
    data = data,
    subPosts = subPosts.associateWith { true },
    count = count,
    route = route,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun generateBasePost(basePostId: String, basePostName: String, subPosts: Set<String>) = Post(
    id = basePostId,
    userId = null,
    title = basePostName,
    content = Constants.EMPTY,
    type = PostType.SUPER,
    media = Constants.EMPTY,
    votes = emptyMap(),
    parentId = Constants.SYSTEM,
    rootId = basePostId,
    status = PostStatus.ACTIVE,
    data = Constants.EMPTY,
    subPosts = subPosts,
    count = 0,
    route = Constants.EMPTY,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

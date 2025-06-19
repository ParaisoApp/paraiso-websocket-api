package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.SiteRoute
import com.paraiso.domain.users.UserReturn
import com.paraiso.domain.util.Constants
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Post(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: PostType,
    val media: String?,
    val votes: Map<String, Boolean>,
    val parentId: String,
    val rootId: String,
    val status: PostStatus,
    val data: String?,
    val subPosts: Set<String>,
    val route: SiteRoute,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class PostReturn(
    val id: String,
    val userId: String,
    val user: UserReturn?,
    val title: String,
    val content: String,
    val type: PostType,
    val media: String?,
    val votes: Map<String, Boolean>,
    val parentId: String,
    val rootId: String,
    val status: PostStatus,
    val data: String?,
    var subPosts: Map<String, PostReturn>,
    val route: SiteRoute,
    val createdOn: Instant,
    val updatedOn: Instant
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

fun Post.toPostReturn(user: UserReturn?) = PostReturn(
    id = id,
    userId = userId,
    user = user,
    title = title,
    content = content,
    type = type,
    media = media,
    votes = votes,
    parentId = parentId,
    rootId = rootId,
    status = status,
    data = data,
    subPosts = emptyMap(),
    route = route,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun generateBasePost(basePostId: String, basePostName: String, subPosts: Set<String>) = Post(
    id = basePostId,
    userId = Constants.SYSTEM,
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
    route = SiteRoute.HOME,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

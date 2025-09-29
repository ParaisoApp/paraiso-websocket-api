package com.paraiso.domain.posts

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    @SerialName(ID) val id: String?,
    val userId: String? = null,
    val title: String?,
    val content: String?,
    val type: PostType,
    val score: Int = 0,
    val parentId: String?,
    val rootId: String?,
    val status: PostStatus = PostStatus.ACTIVE,
    val media: String? = null,
    val data: String?,
    val subPosts: Set<String> = emptySet(),
    val count: Int = 0,
    val route: String? = null,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant? = Clock.System.now(),
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant? = Clock.System.now()
) { companion object }

@Serializable
data class PostResponse(
    val id: String?,
    val userId: String?,
    val title: String?,
    val content: String?,
    val type: PostType,
    val score: Int,
    val parentId: String?,
    val rootId: String?,
    val status: PostStatus,
    val media: String?,
    val data: String?,
    var subPosts: Map<String, Boolean>,
    val count: Int,
    val route: String?,
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

    @SerialName("EVENT")
    EVENT
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

    @SerialName("RISING")
    RISING,

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

fun Post.toResponse() = PostResponse(
    id = id,
    userId = userId,
    title = title,
    content = content,
    type = type,
    media = media,
    score = score,
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

fun generateBasePost(id: String?, name: String?, subPosts: Set<String>) = Post(
    id = id,
    userId = null,
    title = name,
    content = null,
    type = PostType.SUPER,
    media = null,
    score = 0,
    parentId = Constants.SYSTEM,
    rootId = id,
    status = PostStatus.ACTIVE,
    data = null,
    subPosts = subPosts,
    count = 0,
    route = null,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

package com.paraiso.domain.posts

import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String?,
    val userId: String?,
    val title: String?,
    val content: String?,
    val type: PostType,
    val score: Int,
    val parentId: String?,
    val rootId: String?,
    val status: ActiveStatus,
    val media: String?,
    val data: String?,
    val count: Int,
    val route: String?,
    val userVote: Boolean?,
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
enum class ActiveStatus {
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

fun generateBasePost(id: String?, title: String?) = Post(
    id = id,
    userId = null,
    title = title,
    content = null,
    type = PostType.SUPER,
    media = null,
    score = 0,
    parentId = Constants.SYSTEM,
    rootId = id,
    status = ActiveStatus.ACTIVE,
    data = null,
    count = 0,
    route = null,
    userVote = null,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

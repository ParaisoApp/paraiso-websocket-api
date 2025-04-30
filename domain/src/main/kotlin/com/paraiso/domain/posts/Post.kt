package com.paraiso.domain.posts

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
    val status: PostStatus,
    val data: String?,
    val subPosts: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class PostReturn(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: PostType,
    val media: String?,
    val votes: Map<String, Boolean>,
    val parentId: String,
    val status: PostStatus,
    val data: String?,
    var subPosts: Map<String, PostReturn>,
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
    @SerialName("New")
    New,
    @SerialName("Hot")
    Hot,
    @SerialName("Top")
    Top
}

@Serializable
enum class FilterType {
    @SerialName("Posts")
    Posts,
    @SerialName("Comments")
    Comments,
    @SerialName("Users")
    Users,
    @SerialName("Guests")
    Guests
}

@Serializable
enum class Range {
    @SerialName("Day")
    Day,
    @SerialName("Week")
    Week,
    @SerialName("Month")
    Month,
    @SerialName("Year")
    Year,
    @SerialName("All")
    All
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
    status = status,
    data = data,
    subPosts = emptyMap(),
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
    status = PostStatus.ACTIVE,
    data = Constants.EMPTY,
    subPosts = subPosts,
    createdOn = Clock.System.now(),
    updatedOn = Clock.System.now()
)

package com.paraiso.database.posts

import com.paraiso.domain.posts.Post as PostDomain
import com.paraiso.domain.posts.ActiveStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Post(
    @SerialName(Constants.ID) val id: String?,
    val userId: String? = null,
    val title: String?,
    val content: String?,
    val type: PostType,
    val score: Int,
    val parentId: String?,
    val rootId: String?,
    val status: ActiveStatus,
    val media: String? = null,
    val data: String?,
    val count: Int,
    val route: String? = null,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant? = Clock.System.now(),
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant? = Clock.System.now()
) { companion object }
fun PostDomain.toEntity() = Post(
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
    count = count,
    route = route,
    createdOn = createdOn,
    updatedOn = updatedOn
)
fun Post.toDomain() = PostDomain(
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
    count = count,
    route = route,
    userVote = null,
    createdOn = createdOn,
    updatedOn = updatedOn
)
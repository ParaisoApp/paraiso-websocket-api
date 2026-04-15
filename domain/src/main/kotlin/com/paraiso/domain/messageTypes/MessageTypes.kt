package com.paraiso.domain.messageTypes

import com.paraiso.domain.follows.Follow
import com.paraiso.domain.posts.PostPin
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.Favorite
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.votes.Vote
import kotlinx.serialization.Serializable

sealed interface ServerEvent {
    data class MessageReceived(val data: Message) : ServerEvent
    data class VoteReceived(val data: Vote) : ServerEvent
    data class FollowReceived(val data: Follow) : ServerEvent
    data class FavoriteReceived(val data: Favorite) : ServerEvent
    data class DeleteReceived(val data: Delete) : ServerEvent
    data class UserUpdateReceived(val data: User) : ServerEvent
    data class RouteUpdateReceived(val data: RouteUpdate) : ServerEvent
    data class PostPinReceived(val data: PostPin) : ServerEvent
    data class RoleUpdateReceived(val data: RoleUpdate) : ServerEvent
    data class BanReceived(val data: Ban) : ServerEvent
    data class TagReceived(val data: Tag) : ServerEvent
    data class UserReportReceived(val data: Report): ServerEvent
    data class PostReportReceived(val data: Report): ServerEvent
    data class BasicReceived(val data: String): ServerEvent
}

@Serializable
data class Ban(
    val userId: String
)

@Serializable
data class Delete(
    val postId: String,
    val parentId: String
)

@Serializable
data class RouteUpdate(
    val id: String,
    val title: String
)

@Serializable
data class FilterTypes(
    val postTypes: Set<PostType>,
    val userRoles: Set<UserRole>,
    val postIds: Set<String>
) { companion object }

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
data class SubscriptionInfo(
    val userId: String,
    val sessionId: String,
    val type: MessageType,
    val ids: Set<String>,
    val subscribe: Boolean
)

@Serializable
data class Subscription(
    val type: MessageType,
    val ids: Set<String>,
    val subscribe: Boolean
)

@Serializable
data class RoleUpdate(
    val userId: String,
    val userRole: UserRole
)

fun SubscriptionInfo.toSubscription() =
    Subscription(
        type = type,
        ids = ids,
        subscribe = subscribe
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
    ),
    setOf()
)

package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserRole
import kotlinx.serialization.Serializable

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
data class FilterTypes(
    val postTypes: Set<PostType>,
    val userRoles: Set<UserRole>,
    val postIds: Set<String>
) { companion object }

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

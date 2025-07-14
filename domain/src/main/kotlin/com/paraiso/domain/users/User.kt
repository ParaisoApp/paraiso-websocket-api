package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.EMPTY
import com.paraiso.domain.util.Constants.SYSTEM
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val posts: Set<String>,
    val replies: Set<String>,
    val followers: Set<String>,
    val following: Set<String>,
    val chats: Map<String, List<DirectMessage>>,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
    val blockList: Set<String>,
    val image: String,
    val lastSeen: Long,
    val settings: UserSettings,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class UserSettings(
    val theme: Int,
    val accent: Int,
    val toolTips: Boolean,
    val postSubmit: Boolean,
    val showPostUserName: Boolean
) { companion object }

@Serializable
data class UserChat(
    val user: UserReturn?,
    val dmMap: Map<String, DirectMessage>
)

@Serializable
data class UserReturn(
    val id: String,
    val name: String,
    val posts: Map<String, Map<String, Boolean>>,
    val comments: Map<String, Map<String, Boolean>>,
    val replies: Map<String, Boolean>,
    val followers: Map<String, Boolean>,
    val following: Map<String, Boolean>,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
    val blockList: Set<String>,
    val image: String,
    val lastSeen: Long,
    val settings: UserSettings,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

fun User.toUserReturn(
    posts: Map<String, Map<String, Boolean>>,
    comments: Map<String, Map<String, Boolean>>
) =
    UserReturn(
        id = id,
        name = name,
        posts = posts,
        comments = comments,
        replies = replies.associateWith { true },
        followers = followers.associateWith { true },
        following = following.associateWith { true },
        roles = roles,
        banned = banned,
        status = status,
        blockList = blockList,
        image = image,
        lastSeen = lastSeen,
        settings = settings,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserReturn.Companion.systemUser() =
    Clock.System.now().let { now ->
        UserReturn(
            id = SYSTEM,
            name = SYSTEM,
            posts = emptyMap(),
            comments = emptyMap(),
            replies = emptyMap(),
            followers = emptyMap(),
            following = emptyMap(),
            roles = UserRole.SYSTEM,
            banned = false,
            status = UserStatus.CONNECTED,
            blockList = emptySet(),
            image = EMPTY,
            lastSeen = now.toEpochMilliseconds(),
            settings = UserSettings.initSettings(),
            createdOn = now,
            updatedOn = now
        )
    }

fun UserSettings.Companion.initSettings() =
    UserSettings(
        theme = 0,
        accent = 0,
        toolTips = true,
        postSubmit = true,
        showPostUserName = true
    )

fun buildUser(user: User) =
    user.let {
        val posts = mutableMapOf<String, Map<String, Boolean>>()
        val comments = mutableMapOf<String, Map<String, Boolean>>()
        ServerState.posts
            .filterKeys { user.posts.contains(it) }
            .values
            .map { post ->
                if (post.type == PostType.SUB) {
                    posts[post.id] = post.votes
                } else {
                    comments[post.id] = post.votes
                }
            }
        user.toUserReturn(posts, comments)
    }

@Serializable
enum class UserRole {
    @SerialName("ADMIN")
    ADMIN,

    @SerialName("SYSTEM")
    SYSTEM,

    @SerialName("MOD")
    MOD,

    @SerialName("USER")
    USER,

    @SerialName("GUEST")
    GUEST
}

@Serializable
enum class UserStatus {
    @SerialName("CONNECTED")
    CONNECTED,

    @SerialName("DISCONNECTED")
    DISCONNECTED
}

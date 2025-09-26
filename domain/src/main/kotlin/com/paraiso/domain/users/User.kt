package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.SYSTEM
import com.paraiso.domain.util.InstantBsonSerializer
import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName(ID) val id: String,
    val name: String?,
    val fullName: String?,
    val email: String?,
    val about: String?,
    val location: Location?,
    val birthday: Instant?,
    val posts: Map<String, Map<String, Boolean>>,
    val replies: Map<String, Boolean>,
    val followers: Set<String>,
    val following: Set<String>,
    val routeFavorites: Map<String, UserFavorite>,
    val userReports: Map<String, Boolean>,
    val postReports: Map<String, Boolean>,
    val chats: Map<String, ChatRef>,
    val roles: UserRole,
    val banned: Boolean,
    val blockList: Set<String>,
    val image: UserImage,
    val tag: String?,
    val settings: UserSettings,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant
) { companion object }

@Serializable
data class ChatRef(
    val mostRecentDm: DirectMessage,
    val chatId: String,
    val viewed: Boolean
) { companion object }

@Serializable
data class UserSettings(
    val theme: Int,
    val accent: Int,
    val display: Int,
    val toolTips: Boolean,
    val postSubmit: Boolean,
    val openMenus: Boolean,
    val showEmail: Boolean,
    val showName: Boolean,
    val showLocation: Boolean,
    val showBirthday: Boolean
) { companion object }

@Serializable
data class UserImage(
    val url: String?,
    val posX: Int,
    val posY: Int,
    val scale: Float
) { companion object }

@Serializable
data class UserResponse(
    val id: String,
    val name: String?,
    val fullName: String?,
    val email: String?,
    val about: String?,
    val location: Location?,
    val birthday: Instant?,
    val posts: Map<String, Map<String, Boolean>>,
    val chats: Map<String, ChatRef>,
    val replies: Map<String, Boolean>,
    val followers: Map<String, Boolean>,
    val following: Map<String, Boolean>,
    val routeFavorites: Map<String, UserFavorite>,
    val userReports: Map<String, Boolean>,
    val postReports: Map<String, Boolean>,
    val roles: UserRole,
    val banned: Boolean,
    val blockList: Map<String, Boolean>,
    val image: UserImage,
    val tag: String?,
    val settings: UserSettings,
    val status: UserStatus?,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class UserNotifs(
    val replyIds: Set<String>,
    val userChatIds: Set<String>
) { companion object }

@Serializable
data class UserReportNotifs(
    val userIds: Set<String>,
    val postIds: Set<String>
) { companion object }

@Serializable
data class UserFavorite(
    val favorite: Boolean,
    val icon: String?
)

fun User.toResponse(status: UserStatus?) =
    UserResponse(
        id = id,
        name = name,
        fullName = fullName,
        email = email,
        about = about,
        location = location,
        birthday = birthday,
        posts = posts,
        chats = chats,
        replies = replies,
        followers = followers.associateWith { true },
        following = following.associateWith { true },
        routeFavorites = routeFavorites,
        userReports = userReports,
        postReports = postReports,
        roles = roles,
        banned = banned,
        blockList = blockList.associateWith { true },
        image = image,
        settings = settings,
        tag = tag,
        status = status,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

//user list doesn't need all data
fun User.toBasicResponse(status: UserStatus?) =
    UserResponse(
        id = id,
        name = name,
        fullName = fullName,
        email = email,
        about = about,
        location = location,
        birthday = birthday,
        posts = posts,
        chats = chats,
        replies = replies,
        followers = followers.associateWith { true },
        following = following.associateWith { true },
        routeFavorites = routeFavorites,
        userReports = emptyMap(), // Default
        postReports = emptyMap(), // Default
        roles = roles,
        banned = false, // Default
        blockList = emptyMap(), // Default
        image = image,
        settings = settings,
        tag = tag,
        status = status,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserResponse.toUser() =
    User(
        id = id,
        name = name,
        fullName = fullName,
        email = email,
        about = about,
        location = location,
        birthday = birthday,
        posts = posts,
        replies = replies,
        followers = followers.keys,
        following = following.keys,
        routeFavorites = routeFavorites,
        userReports = userReports,
        postReports = postReports,
        chats = chats,
        roles = roles,
        banned = banned,
        blockList = blockList.keys,
        image = image,
        settings = settings,
        tag = tag,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun randomGuestName() = "Guest${(Math.random() * 10000).toInt()}"

fun UserResponse.Companion.newUser(
    id: String
) =
    Clock.System.now().let { now ->
        UserResponse(
            id = id,
            name = randomGuestName(),
            fullName = null,
            email = null,
            about = null,
            location = null,
            birthday = null,
            posts = emptyMap(),
            replies = emptyMap(),
            chats = emptyMap(),
            followers = emptyMap(),
            following = emptyMap(),
            routeFavorites = emptyMap(),
            userReports = emptyMap(),
            postReports = emptyMap(),
            roles = UserRole.GUEST,
            banned = false,
            blockList = emptyMap(),
            image = UserImage.initImage(),
            settings = UserSettings.initSettings(),
            tag = null,
            status = UserStatus.CONNECTED,
            createdOn = now,
            updatedOn = now
        )
    }
fun UserResponse.Companion.systemUser() =
    Clock.System.now().let { now ->
        UserResponse(
            id = SYSTEM,
            name = SYSTEM,
            fullName = null,
            email = null,
            about = null,
            location = null,
            birthday = null,
            posts = emptyMap(),
            replies = emptyMap(),
            chats = emptyMap(),
            followers = emptyMap(),
            following = emptyMap(),
            routeFavorites = emptyMap(),
            userReports = emptyMap(),
            postReports = emptyMap(),
            roles = UserRole.ADMIN,
            banned = false,
            blockList = emptyMap(),
            image = UserImage.initImage(),
            settings = UserSettings.initSettings(),
            tag = null,
            status = UserStatus.DISCONNECTED,
            createdOn = now,
            updatedOn = now
        )
    }

fun UserSettings.Companion.initSettings() =
    UserSettings(
        theme = 0,
        accent = 0,
        display = 0,
        toolTips = true,
        postSubmit = true,
        openMenus = true,
        showEmail = false,
        showName = true,
        showLocation = true,
        showBirthday = true
    )

fun UserImage.Companion.initImage() =
    UserImage(
        url = null,
        posX = 0,
        posY = 0,
        scale = 1f
    )

@Serializable
enum class UserRole {
    @SerialName("ADMIN")
    ADMIN,

    @SerialName("SYSTEM")
    SYSTEM,

    @SerialName("MOD")
    MOD,

    @SerialName("FOLLOWING")
    FOLLOWING,

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

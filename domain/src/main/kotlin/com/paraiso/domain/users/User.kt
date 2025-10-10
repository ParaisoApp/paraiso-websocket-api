package com.paraiso.domain.users

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.SYSTEM
import com.paraiso.domain.util.InstantBsonSerializer
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
    val replies: Int,
    val score: Int,
    val followers: Int,
    val following: Int,
    val routeFavorites: Map<String, UserFavorite>,
    val reports: Int,
    val chats: Int,
    val roles: UserRole,
    val banned: Boolean,
    val image: UserImage,
    val tag: String?,
    val settings: UserSettings,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant
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
    val chats: Int,
    val replies: Int,
    val score: Int,
    val followers: Int,
    val following: Int,
    val routeFavorites: Map<String, UserFavorite>,
    val reports: Int,
    val roles: UserRole,
    val banned: Boolean,
    val image: UserImage,
    val tag: String?,
    val settings: UserSettings,
    val status: UserStatus?,
    val viewerContext: ViewerContext,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class UserNotifs(
    val replyIds: Set<String>
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

@Serializable
data class ViewerContext(
    val following: Boolean?,
    val blocking: Boolean?
)

fun User.toResponse(status: UserStatus?, viewerContext: ViewerContext) =
    UserResponse(
        id = id,
        name = name,
        fullName = fullName,
        email = email,
        about = about,
        location = location,
        birthday = birthday,
        chats = chats,
        replies = replies,
        score = score,
        followers = followers,
        following = following,
        routeFavorites = routeFavorites,
        reports = reports,
        roles = roles,
        banned = banned,
        image = image,
        settings = settings,
        tag = tag,
        status = status,
        viewerContext = viewerContext,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

// user list doesn't need all data
fun User.toBasicResponse(status: UserStatus?, viewerContext: ViewerContext) =
    UserResponse(
        id = id,
        name = name,
        fullName = fullName,
        email = email,
        about = about,
        location = location,
        birthday = birthday,
        chats = chats,
        replies = replies,
        score = score,
        followers = followers,
        following = following,
        routeFavorites = routeFavorites,
        reports = 0, // Default
        roles = roles,
        banned = false, // Default
        image = image,
        settings = settings,
        tag = tag,
        status = status,
        viewerContext = viewerContext,
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
        replies = replies,
        score = score,
        followers = followers,
        following = following,
        routeFavorites = routeFavorites,
        reports = reports,
        chats = chats,
        roles = roles,
        banned = banned,
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
            replies = 0,
            score = 0,
            chats = 0,
            followers = 0,
            following = 0,
            routeFavorites = emptyMap(),
            reports = 0,
            roles = UserRole.GUEST,
            banned = false,
            image = UserImage.initImage(),
            settings = UserSettings.initSettings(),
            tag = null,
            status = UserStatus.CONNECTED,
            viewerContext = ViewerContext(
                following = false,
                blocking = false
            ),
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
            replies = 0,
            score = 0,
            chats = 0,
            followers = 0,
            following = 0,
            routeFavorites = emptyMap(),
            reports = 0,
            roles = UserRole.ADMIN,
            banned = false,
            image = UserImage.initImage(),
            settings = UserSettings.initSettings(),
            tag = null,
            status = UserStatus.DISCONNECTED,
            viewerContext = ViewerContext(
                following = false,
                blocking = false
            ),
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

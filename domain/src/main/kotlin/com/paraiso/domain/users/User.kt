package com.paraiso.domain.users

import com.paraiso.domain.auth.AuthId
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
    val authIds: List<AuthId>,
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
    val tipLinks: Map<String, String>,
    val socialLinks: Map<String, String>,
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
    val textSize: Int,
    val toolTips: Boolean,
    val postSubmit: Boolean,
    val openMenus: Boolean,
    val showEmail: Boolean,
    val showName: Boolean,
    val showLocation: Boolean,
    val showBirthday: Boolean,
    val hidden: Boolean
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
    val tipLinks: Map<String, String>,
    val socialLinks: Map<String, String>,
    val status: UserStatus?,
    val viewerContext: ViewerContext,
    val sessionId: String?,
    val createdOn: Instant,
    val updatedOn: Instant
) { companion object }

@Serializable
data class UserFavorite(
    val routeId: String,
    val route: String,
    val modifier: String?,
    val title: String,
    val favorite: Boolean,
    val icon: String?
)

@Serializable
data class ViewerContext(
    val following: Boolean?,
    val blocking: Boolean?
)

// user list doesn't need all data
fun User.toBasicResponse(status: UserStatus?, viewerContext: ViewerContext) =
    UserResponse(
        id = id,
        name = name,
        fullName = if(settings.showName) fullName else null,
        email = if(settings.showEmail) email else null,
        about = about,
        location = if(settings.showLocation) location else null,
        birthday = if(settings.showBirthday) birthday else null,
        chats = chats,
        replies = replies,
        score = score,
        followers = followers,
        following = following,
        routeFavorites = routeFavorites,
        reports = 0, // Default - hide info
        roles = roles,
        banned = false, // Default - hide info
        image = image,
        settings = settings,
        tipLinks = tipLinks,
        socialLinks = socialLinks,
        tag = tag,
        status = status,
        viewerContext = viewerContext,
        sessionId = null,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun UserResponse.toUser() =
    User(
        id = id,
        authIds = emptyList(),
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
        tipLinks = tipLinks,
        socialLinks = socialLinks,
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
            tipLinks = emptyMap(),
            socialLinks = emptyMap(),
            viewerContext = ViewerContext(
                following = false,
                blocking = false
            ),
            sessionId = null, // session id used to tie event data to user's tab
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
            tipLinks = emptyMap(),
            socialLinks = emptyMap(),
            viewerContext = ViewerContext(
                following = false,
                blocking = false
            ),
            sessionId = null,
            createdOn = now,
            updatedOn = now
        )
    }

fun UserSettings.Companion.initSettings() =
    UserSettings(
        theme = 0,
        accent = 0,
        display = 0,
        textSize = 0,
        toolTips = true,
        postSubmit = true,
        openMenus = true,
        showEmail = false,
        showName = false,
        showLocation = false,
        showBirthday = false,
        hidden = false
    )

fun UserImage.Companion.initImage() =
    UserImage(
        url = null,
        posX = 0,
        posY = 0,
        scale = 1f
    )

fun User.toResponse(sessionId: String?, status: UserStatus?, viewerContext: ViewerContext) = UserResponse(
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
    tipLinks = tipLinks,
    socialLinks = socialLinks,
    tag = tag,
    status = status,
    viewerContext = viewerContext,
    sessionId = sessionId,
    createdOn = createdOn,
    updatedOn = updatedOn
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

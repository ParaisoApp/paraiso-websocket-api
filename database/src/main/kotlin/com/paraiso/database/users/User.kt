package com.paraiso.database.users

import com.paraiso.domain.auth.AuthId
import com.paraiso.domain.users.Location
import com.paraiso.domain.users.User as UserDomain
import com.paraiso.domain.users.UserFavorite as UserFavoriteDomain
import com.paraiso.domain.users.UserImage as UserImageDomain
import com.paraiso.domain.users.UserSettings as UserSettingsDomain
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.ViewerContext
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
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
    val routeFavorites: List<UserFavorite>,
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

fun UserDomain.toEntity() = User(
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
    routeFavorites = routeFavorites.values.map { it.toEntity() },
    reports = reports,
    chats = chats,
    roles = roles,
    banned = banned,
    image = image.toEntity(),
    settings = settings.toEntity(),
    tipLinks = tipLinks,
    socialLinks = socialLinks,
    tag = tag,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun UserFavoriteDomain.toEntity() = UserFavorite(
    routeId = routeId,
    route = route,
    modifier = modifier,
    title = title,
    favorite = favorite,
    icon= icon
)

fun UserImageDomain.toEntity() = UserImage(
    url = url,
    posX = posX,
    posY = posY,
    scale = scale
)

fun UserSettingsDomain.toEntity() = UserSettings(
    theme = theme,
    accent = accent,
    display = display,
    textSize = textSize,
    toolTips = toolTips,
    postSubmit = postSubmit,
    openMenus = openMenus,
    showEmail = showEmail,
    showName = showName,
    showLocation = showLocation,
    showBirthday = showBirthday,
    hidden = hidden,
)

fun User.toDomain() = UserDomain(
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
    routeFavorites = routeFavorites.associate { it.routeId to it.toDomain() },
    reports = reports,
    chats = chats,
    roles = roles,
    banned = banned,
    image = image.toDomain(),
    settings = settings.toDomain(),
    tipLinks = tipLinks,
    socialLinks = socialLinks,
    tag = tag,
    status = null,
    viewerContext = null,
    sessionId = null,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun UserFavorite.toDomain() = UserFavoriteDomain(
    routeId = routeId,
    route = route,
    modifier = modifier,
    title = title,
    favorite = favorite,
    icon= icon
)

fun UserImage.toDomain() = UserImageDomain(
    url = url,
    posX = posX,
    posY = posY,
    scale = scale
)

fun UserSettings.toDomain() = UserSettingsDomain(
    theme = theme,
    accent = accent,
    display = display,
    textSize = textSize,
    toolTips = toolTips,
    postSubmit = postSubmit,
    openMenus = openMenus,
    showEmail = showEmail,
    showName = showName,
    showLocation = showLocation,
    showBirthday = showBirthday,
    hidden = hidden,
)

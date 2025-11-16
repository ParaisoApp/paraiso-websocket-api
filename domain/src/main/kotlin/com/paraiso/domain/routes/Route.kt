package com.paraiso.domain.routes

import com.paraiso.domain.posts.PostResponse
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteDetails(
    @SerialName(ID) val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Int,
    val about: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

@Serializable
data class RouteResponse(
    val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Int,
    val about: String?,
    val pinnedPostIds: List<String>,
    val pinnedPosts: Map<String, PostResponse>,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

@Serializable
data class Favorite(
    val userId: String?,
    val route: String,
    val icon: String?,
    val favorite: Boolean,
    val toggle: Boolean
)

@Serializable
data class Route(
    val route: SiteRoute,
    val modifier: SiteRoute,
    val content: String?
)

@Serializable
enum class SiteRoute {
    @SerialName("HOME")
    HOME,

    @SerialName("PROFILE")
    PROFILE,

    @SerialName("SPORT")
    SPORT,

    @SerialName("TEAM")
    TEAM,

    @SerialName("FOOTBALL")
    FOOTBALL,

    @SerialName("BASKETBALL")
    BASKETBALL,

    @SerialName("HOCKEY")
    HOCKEY,

    @SerialName("BASEBALL")
    BASEBALL,

    @SerialName("SOCCER")
    SOCCER,

    @SerialName("TENNIS")
    TENNIS,

    @SerialName("GOLF")
    GOLF
}
fun RouteDetails.toResponse(pinnedPostIds: List<String>, pinnedPosts: Map<String, PostResponse>) =
    RouteResponse(
        id = id,
        route = route,
        modifier = modifier,
        title = title,
        userFavorites = userFavorites,
        about = about,
        pinnedPostIds = pinnedPostIds,
        pinnedPosts = pinnedPosts,
        createdOn = createdOn,
        updatedOn = updatedOn
    )

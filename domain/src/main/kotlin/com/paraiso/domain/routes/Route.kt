package com.paraiso.domain.routes

import com.paraiso.domain.posts.Post
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteDetails(
    val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Int,
    val about: String?,
    val pinnedPostIds: List<String>,
    val pinnedPosts: Map<String, Post>,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

@Serializable
data class Favorite(
    val userId: String?,
    val routeId: String,
    val route: String,
    val modifier: String?,
    val title: String,
    val icon: String?,
    val favorite: Boolean,
    val toggle: Boolean
)

@Serializable
data class Route(
    val routeId: String,
    val route: SiteRoute,
    val modifier: SiteRoute,
    val content: String?
)

@Serializable
enum class SiteRoute {
    @SerialName("HOME")
    HOME,

    @SerialName("FAVORITES")
    FAVORITES,

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

fun isSportRoute(route: String) = route == SiteRoute.FOOTBALL.name || route == SiteRoute.BASKETBALL.name || route == SiteRoute.HOCKEY.name

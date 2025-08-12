package com.paraiso.domain.routes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteDetails(
    val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Set<String>,
    val about: String?
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

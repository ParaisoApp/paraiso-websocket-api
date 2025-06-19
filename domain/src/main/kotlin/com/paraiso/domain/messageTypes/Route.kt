package com.paraiso.domain.messageTypes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val route: SiteRoute,
    val content: String
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

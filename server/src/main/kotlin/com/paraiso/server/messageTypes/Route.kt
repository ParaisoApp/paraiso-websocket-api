package com.paraiso.server.messageTypes

import com.paraiso.domain.messageTypes.SiteRoute
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
    TEAM
}

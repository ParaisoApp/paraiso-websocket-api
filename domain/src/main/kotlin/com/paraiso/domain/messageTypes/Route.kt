package com.paraiso.domain.messageTypes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val route: SocketRoute,
    val content: String
)

@Serializable
enum class SocketRoute {
    @SerialName("HOME")
    HOME,

    @SerialName("PROFILE")
    PROFILE,

    @SerialName("SPORT")
    SPORT,

    @SerialName("TEAM")
    TEAM
}

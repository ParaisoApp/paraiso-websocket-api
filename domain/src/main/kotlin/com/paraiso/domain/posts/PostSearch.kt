package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.routes.RouteResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayOps(
    val range: Range,
    val sort: SortType,
    val selectedFilters: FilterTypes
)

@Serializable
data class InitSearch(
    val routeId: String,
    val postsDisplayOps: DisplayOps,
    val userId: String,
    val sessionId: String,
    val postId: String?
)

@Serializable
data class PostSearch(
    val route: RouteResponse,
    val postsDisplayOps: DisplayOps,
    val userId: String,
    val sessionId: String
)

@Serializable
data class PostSearchId(
    val id: String,
    val postsDisplayOps: DisplayOps,
    val userId: String,
    val sessionId: String,
    val gameState: GameState? = null,
    val commentRouteLocation: String? = null
)

@Serializable
enum class GameState {
    @SerialName("PRE")
    PRE,

    @SerialName("MID")
    MID,

    @SerialName("POST")
    POST,

    @SerialName("ALL")
    ALL
}

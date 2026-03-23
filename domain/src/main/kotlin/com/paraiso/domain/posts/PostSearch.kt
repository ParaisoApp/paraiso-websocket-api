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
data class InitSearchRequest(
    val routeId: String,
    val postsDisplayOps: DisplayOps,
    val contentDisplayOps: DisplayOps,
    val sessionId: String,
    val postId: String?
)

@Serializable
data class InitSearch(
    val routeId: String,
    val postsDisplayOps: DisplayOps,
    val contentDisplayOps: DisplayOps,
    val userId: String,
    val sessionId: String,
    val postId: String?
)

@Serializable
data class PostSearchRequest(
    val route: RouteResponse,
    val postsDisplayOps: DisplayOps,
    val sessionId: String
)

@Serializable
data class PostSearch(
    val route: RouteResponse,
    val postsDisplayOps: DisplayOps,
    val userId: String,
    val sessionId: String
)

@Serializable
data class PostSearchIdRequest(
    val id: String,
    val postsDisplayOps: DisplayOps,
    val sessionId: String,
    val gameState: GameState? = null,
    val commentRouteLocation: String? = null
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

fun InitSearchRequest.toDomain(userId: String) = InitSearch(
    routeId = routeId,
    postsDisplayOps = postsDisplayOps,
    contentDisplayOps = contentDisplayOps,
    userId = userId,
    sessionId = sessionId,
    postId = postId,
)

fun PostSearchRequest.toDomain(userId: String) = PostSearch(
    route = route,
    postsDisplayOps = postsDisplayOps,
    sessionId = sessionId,
    userId = userId,
)

fun PostSearchIdRequest.toDomain(userId: String) = PostSearchId(
    id = id,
    postsDisplayOps = postsDisplayOps,
    sessionId = sessionId,
    gameState = gameState,
    commentRouteLocation = commentRouteLocation,
    userId = userId,
)

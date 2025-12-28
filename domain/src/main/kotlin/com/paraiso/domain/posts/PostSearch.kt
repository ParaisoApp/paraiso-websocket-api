package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.FilterTypes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostSearch(
    val id: String,
    val name: String,
    val range: Range,
    val sort: SortType,
    val selectedFilters: FilterTypes,
    val userId: String,
    val sessionId: String,
)

@Serializable
data class PostSearchId(
    val id: String,
    val range: Range,
    val sort: SortType,
    val selectedFilters: FilterTypes,
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
    ALL,
}

package com.paraiso.domain.posts

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.routes.RouteResponse
import com.paraiso.domain.users.UserFavorite
import kotlinx.datetime.Instant

interface PostsDB {
    suspend fun findById(id: String): Post?
    suspend fun findByIdsIn(ids: Set<String>): List<Post>
    suspend fun findByPartial(partial: String): List<Post>
    suspend fun findByUserId(userId: String): List<Post>
    suspend fun findByBaseCriteria(
        route: RouteResponse?,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFavorites: List<UserFavorite>,
        userFollowing: Set<String>
    ): List<Post>
    suspend fun findByParentId(
        parentId: String,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>
    ): List<Post>
    suspend fun findByParentIdWithEventFilters(
        parentId: String,
        range: Instant?,
        filters: FilterTypes,
        sortType: SortType,
        userFollowing: Set<String>,
        compStartTime: Instant?,
        compEndTime: Instant?,
        gameState: GameState?,
        commentRouteLocation: String?
    ): List<Post>
    suspend fun save(posts: List<Post>): Int
    suspend fun saveIfNew(posts: List<Post>): Int
    suspend fun editPost(message: Message): Long
    suspend fun setPostDeleted(
        id: String
    ): Long
    suspend fun setScore(
        id: String,
        score: Int
    ): Long
    suspend fun setCount(
        id: String,
        increment: Int
    ): Long
}

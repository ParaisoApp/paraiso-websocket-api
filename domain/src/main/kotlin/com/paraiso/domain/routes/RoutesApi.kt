package com.paraiso.domain.routes

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.posts.InitRouteData
import com.paraiso.domain.posts.PostPinsApi
import com.paraiso.domain.posts.PostResponse
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.posts.Range
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.GAME_PREFIX
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoutesApi(
    private val routesDB: RoutesDB,
    private val postsApi: PostsApi,
    private val postPinsApi: PostPinsApi,
    private val userSessionsApi: UserSessionsApi
) {
    suspend fun getById(id: String, userId: String, sessionId: String): RouteResponse? {
        postPinsApi.findByRouteId(id).let {pinnedPosts ->
            val postIds = pinnedPosts.map { it.postId }
            val posts = postsApi.getByIds(userId, postIds.toSet(), sessionId)
            return routesDB.findById(id)?.toResponse(postIds, posts.posts)
        }
    }
    suspend fun saveRoutes(routeDetails: List<RouteDetails>) = routesDB.save(routeDetails)

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        if (favorite.userId != null) {
            // toggle favorite from Route
            if (!favorite.favorite) {
                routesDB.setFavorites(favorite.routeId, -1)
            } else {
                routesDB.setFavorites(favorite.routeId, 1)
            }
        }
    }

    suspend fun initPage(routeId: String, userId: String, sessionId: String, filters: FilterTypes, postId: String?) = coroutineScope {
        val routeRes = async { getById(routeId, userId, sessionId) }
        val users = async { userSessionsApi.getUserList(filters, userId) }
        routeRes.await()?.let { route ->
            val postsData = postsApi.getPosts(
                routeId,
                route.title,
                Range.DAY,
                SortType.NEW,
                filters,
                userId,
                sessionId
            )
            if(postId != null){
                postsApi.getById(
                    postId,
                    Range.DAY,
                    SortType.NEW,
                    filters,
                    userId,
                    sessionId
                )?.posts?.values?.firstOrNull()?.let{ postById ->
                    postsData.posts[postId] = postById
                }
            }
            InitRouteData(postsData, users.await(), route)
        }
    }
}

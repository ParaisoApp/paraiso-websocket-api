package com.paraiso.domain.routes

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.InitRouteData
import com.paraiso.domain.posts.PostPinsApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.posts.Range
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.users.UserSessionsApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RoutesApi(
    private val routesDB: RoutesDB,
    private val postsApi: PostsApi,
    private val postPinsApi: PostPinsApi,
    private val userSessionsApi: UserSessionsApi
) {
    suspend fun getById(id: String, userId: String): RouteResponse? {
        postPinsApi.findByRouteId(id).let {pinnedPosts ->
            val postIds = pinnedPosts.map { it.postId }
            val posts = postsApi.getByIds(userId, postIds.toSet())
            return routesDB.findById(id)?.toResponse(postIds, posts)
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

    suspend fun initPage(routeId: String, userId: String, filters: FilterTypes, postId: String?) = coroutineScope {
        val routeRes = async { getById(routeId, userId) }
        val users = async { userSessionsApi.getUserList(filters, userId) }
        routeRes.await()?.let { route ->
            val posts = postsApi.getPosts(
                routeId,
                route.title,
                Range.DAY,
                SortType.NEW,
                filters,
                userId
            )
            if(postId != null){
                postsApi.getById(
                    postId,
                    Range.DAY,
                    SortType.NEW,
                    filters,
                    userId
                )?.values?.firstOrNull()?.let{postById ->
                    posts[postId] = postById
                }
            }
            InitRouteData(posts, users.await(), route)
        }
    }
}

package com.paraiso.domain.routes

import com.paraiso.domain.posts.PostPinsApi
import com.paraiso.domain.posts.PostsApi

class RoutesApi(
    private val routesDB: RoutesDB,
    private val postsApi: PostsApi,
    private val postPinsApi: PostPinsApi
) {
    suspend fun getById(id: String, userId: String): RouteResponse? {
        postPinsApi.findByRouteId(id).let {pinnedPosts ->
            val posts = postsApi.getByIds(userId, pinnedPosts.map { it.postId }.toSet())
            return routesDB.findById(id)?.toResponse(posts)
        }
    }
    suspend fun saveRoutes(routeDetails: List<RouteDetails>) = routesDB.save(routeDetails)

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        if (favorite.userId != null) {
            // toggle favorite from Route
            if (!favorite.favorite) {
                routesDB.setFavorites(favorite.route, -1)
            } else {
                routesDB.setFavorites(favorite.route, 1)
            }
        }
    }
}

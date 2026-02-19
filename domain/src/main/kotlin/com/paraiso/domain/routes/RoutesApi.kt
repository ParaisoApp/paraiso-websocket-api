package com.paraiso.domain.routes

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.InitRouteData
import com.paraiso.domain.posts.InitSearch
import com.paraiso.domain.posts.PostPinsApi
import com.paraiso.domain.posts.PostSearch
import com.paraiso.domain.posts.PostSearchId
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
    suspend fun getById(id: String, userId: String, sessionId: String): RouteResponse? {
        postPinsApi.findByRouteId(id).let { pinnedPosts ->
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

    suspend fun initPage(initSearch: InitSearch) = coroutineScope {
        val routeRes = async { getById(initSearch.routeId, initSearch.userId, initSearch.sessionId) }
        val users = async { userSessionsApi.getUserList(initSearch.selectedFilters, initSearch.userId) }
        routeRes.await()?.let { route ->
            val postsData = postsApi.getPosts(
                PostSearch(
                    route,
                    initSearch.range,
                    initSearch.sort,
                    initSearch.selectedFilters,
                    initSearch.userId,
                    initSearch.sessionId
                )
            )
            if (initSearch.postId != null) {
                postsApi.getById(
                    PostSearchId(
                        initSearch.postId,
                        initSearch.range,
                        initSearch.sort,
                        initSearch.selectedFilters,
                        initSearch.userId,
                        initSearch.sessionId
                    )
                )?.posts?.values?.forEach { post ->
                    post.id?.let {
                        postsData.posts[it] = post
                    }
                }
            }
            InitRouteData(postsData, users.await(), route)
        }
    }
}

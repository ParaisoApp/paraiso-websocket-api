package com.paraiso.domain.routes

interface RoutesDBAdapter {
    suspend fun findById(id: String): RouteDetails?
    suspend fun save(routes: List<RouteDetails>): Int

    suspend fun addUserFavorites(
        route: String,
        userFavoriteId: String
    ): Long

    suspend fun removeUserFavorites(
        route: String,
        userFavoriteId: String
    ): Long
}

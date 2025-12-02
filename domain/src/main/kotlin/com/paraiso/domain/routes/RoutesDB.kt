package com.paraiso.domain.routes

interface RoutesDB {
    suspend fun findById(id: String): RouteDetails?
    suspend fun save(routes: List<RouteDetails>): Int

    suspend fun setFavorites(
        routeId: String,
        favorite: Int
    ): Long
}

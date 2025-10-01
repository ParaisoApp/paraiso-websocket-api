package com.paraiso.domain.routes

interface RoutesDB {
    suspend fun findById(id: String): RouteDetails?
    suspend fun findByUserName(userName: String): RouteDetails?
    suspend fun save(routes: List<RouteDetails>): Int

    suspend fun setFavorites(
        route: String,
        favorite: Int
    ): Long
}

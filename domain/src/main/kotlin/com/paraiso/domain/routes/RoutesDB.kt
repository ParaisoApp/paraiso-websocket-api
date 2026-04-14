package com.paraiso.domain.routes

interface RoutesDB {
    suspend fun findById(id: String): RouteDetails?
    suspend fun save(routes: List<RouteDetails>): Int
    suspend fun setTitle(id: String, title: String): Long
    suspend fun setFavorites(
        id: String,
        favorite: Int
    ): Long
}

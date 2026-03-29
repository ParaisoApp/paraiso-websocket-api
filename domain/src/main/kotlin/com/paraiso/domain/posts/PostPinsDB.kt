package com.paraiso.domain.posts

interface PostPinsDB {
    suspend fun findByRouteId(
        routeId: String
    ): List<Pair<PostPin, String?>>
    suspend fun save(
        postPins: List<PostPin>
    ): Int
    suspend fun delete(
        id: String
    ): Long
}

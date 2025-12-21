package com.paraiso.domain.posts

class PostPinsApi(
    private val postPinsDB: PostPinsDB
) {
    suspend fun findByRouteId(routeId: String) =
        // id as route ID for now (with only one allowed pinned post)
        postPinsDB.findByRouteId(routeId)
    suspend fun save(routeId: String, postId: String, userId: String, order: Int) =
        // id as route ID for now (with only one allowed pinned post)
        postPinsDB.save(listOf(PostPin(routeId, routeId, postId, order, userId)))
    suspend fun delete(id: String) =
        // id as route ID for now (with only one allowed pinned post)
        postPinsDB.delete(id)
}

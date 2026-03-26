package com.paraiso.domain.posts

class PostPinsApi(
    private val postPinsDB: PostPinsDB
) {
    suspend fun findByRouteId(routeId: String) =
        postPinsDB.findByRouteId(routeId)
    suspend fun save(postPin: PostPinResponse) =
        postPinsDB.save(listOf(postPin.toEntity()))
    suspend fun delete(id: String) =
        postPinsDB.delete(id)
}

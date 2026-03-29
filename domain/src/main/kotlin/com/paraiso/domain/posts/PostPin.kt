package com.paraiso.domain.posts

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PostPin(
    val id: String?,
    val routeId: String,
    val post: Post?,
    val order: Int,
    val userId: String?,
    val createdOn: Instant?
)

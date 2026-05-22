package com.paraiso.domain.admin

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserReport(
    val userId: String,
    val reportedBy: Map<String, Boolean>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class PostReport(
    val postId: String,
    val reportedBy: Map<String, Boolean>,
    val createdOn: Instant,
    val updatedOn: Instant
)

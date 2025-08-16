package com.paraiso.domain.admin

import com.paraiso.domain.posts.PostReturn
import com.paraiso.domain.users.UserResponse
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserReport(
    val userId: String,
    val reportedBy: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class PostReport(
    val postId: String,
    val reportedBy: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
)

package com.paraiso.domain.admin

import com.paraiso.domain.posts.PostResponse
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
data class UserReportResponse(
    val user: UserResponse,
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

@Serializable
data class PostReportResponse(
    val post: PostResponse,
    val reportedBy: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
)

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
    val reportedBy: Map<String, Boolean>,
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
    val reportedBy: Map<String, Boolean>,
    val createdOn: Instant,
    val updatedOn: Instant
)

fun UserReport.toResponse(user: UserResponse) =
    UserReportResponse(
        user = user,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun PostReport.toResponse(post: PostResponse) =
    PostReportResponse(
        post = post,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

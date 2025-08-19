package com.paraiso.domain.admin

import com.paraiso.domain.util.Constants.ID
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserReport(
    @SerialName(ID) val id: String,
    val reportedBy: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class UserReportResponse(
    val userId: String,
    val reportedBy: Map<String, Boolean>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class PostReport(
    @SerialName(ID) val id: String,
    val reportedBy: Set<String>,
    val createdOn: Instant,
    val updatedOn: Instant
)

@Serializable
data class PostReportResponse(
    val postId: String,
    val reportedBy: Map<String, Boolean>,
    val createdOn: Instant,
    val updatedOn: Instant
)

fun UserReport.toResponse() =
    UserReportResponse(
        userId = id,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

fun PostReport.toResponse() =
    PostReportResponse(
        postId = id,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )

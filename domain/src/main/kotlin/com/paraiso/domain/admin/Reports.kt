package com.paraiso.domain.admin

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
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

package com.paraiso.database.admin

import com.paraiso.domain.admin.UserReport as UserReportDomain
import com.paraiso.domain.admin.PostReport as PostReportDomain
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserReport(
    @SerialName(ID) val id: String,
    val reportedBy: Set<String>,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant
)

@Serializable
data class PostReport(
    @SerialName(ID) val id: String,
    val reportedBy: Set<String>,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant
)
fun UserReportDomain.toEntity() =
    UserReport(
        id = userId,
        reportedBy = reportedBy.keys,
        createdOn = createdOn,
        updatedOn = updatedOn
    )
fun UserReport.toDomain() =
    UserReportDomain(
        userId = id,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )
fun PostReportDomain.toEntity() =
    PostReport(
        id = postId,
        reportedBy = reportedBy.keys,
        createdOn = createdOn,
        updatedOn = updatedOn
    )
fun PostReport.toDomain() =
    PostReportDomain(
        postId = id,
        reportedBy = reportedBy.associateWith { true },
        createdOn = createdOn,
        updatedOn = updatedOn
    )
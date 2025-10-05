package com.paraiso.domain.notifications

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import com.paraiso.domain.votes.Vote
import com.paraiso.domain.votes.VoteResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName(ID) val id: String?,
    val userId: String,
    val createUserId: String?,
    val refId: String?,
    val replyId: String?,
    val content: String?,
    val type: NotificationType,
    val userRead: Boolean,
    val createUserRead: Boolean,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

@Serializable
data class NotificationResponse(
    val id: String?,
    val userId: String,
    val createUserId: String?,
    val refId: String?,
    val replyId: String?,
    val content: String?,
    val type: NotificationType,
    val userRead: Boolean,
    val createUserRead: Boolean,
    val createdOn: Instant?,
    val updatedOn: Instant?

)


@Serializable
enum class NotificationType {
    @SerialName("POST")
    POST,

    @SerialName("DM")
    DM,

    @SerialName("USER_REPORT")
    USER_REPORT,

    @SerialName("POST_REPORT")
    POST_REPORT,

    @SerialName("FOLLOW")
    FOLLOW,

    @SerialName("VOTE")
    VOTE,
}

fun NotificationResponse.toDomain() = Notification(
    id = id ?: "$userId-$createUserId-$refId-$replyId",
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = content,
    type = type,
    userRead = userRead,
    createUserRead = createUserRead,
    createdOn = createdOn ?: Clock.System.now(),
    updatedOn = updatedOn ?: Clock.System.now()
)

fun Notification.toResponse() = NotificationResponse(
    id = id,
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = content,
    type = type,
    userRead = userRead,
    createUserRead = createUserRead,
    createdOn = createdOn,
    updatedOn = updatedOn
)

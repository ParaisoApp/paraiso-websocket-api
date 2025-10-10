package com.paraiso.domain.notifications

import com.paraiso.domain.posts.PostResponse
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
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
    val content: NotificationContent?,
    val type: NotificationType,
    val userRead: Boolean,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

@Serializable
sealed interface NotificationContent
@Serializable
data class PostNotificationContent(
    val post: PostResponse?
) : NotificationContent

@Serializable
enum class NotificationType {
    @SerialName("POST")
    POST,

    @SerialName("DM")
    DM,

    @SerialName("MENTION")
    MENTION,

    @SerialName("USER_REPORT")
    USER_REPORT,

    @SerialName("POST_REPORT")
    POST_REPORT,

    @SerialName("FOLLOW")
    FOLLOW,

    @SerialName("VOTE")
    VOTE,
}

fun NotificationResponse.toDomain(stringContent: String?) = Notification(
    id = id ?: "$userId-$createUserId-$refId-$replyId",
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = stringContent,
    type = type,
    userRead = userRead,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun Notification.toPostResponse(refContent: PostResponse?) = NotificationResponse(
    id = id,
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = PostNotificationContent(refContent),
    type = type,
    userRead = userRead,
    createdOn = createdOn,
    updatedOn = updatedOn
)

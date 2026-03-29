package com.paraiso.domain.notifications

import com.paraiso.domain.posts.Post
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
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
    val post: Post?
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
    VOTE
}

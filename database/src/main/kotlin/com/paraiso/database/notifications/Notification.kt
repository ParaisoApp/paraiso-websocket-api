package com.paraiso.database.notifications

import com.paraiso.domain.notifications.Notification as NotificationDomain
import com.paraiso.domain.notifications.NotificationType
import com.paraiso.domain.posts.PostResponse
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
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
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)

fun NotificationDomain.toEntity(content: String?) = Notification(
    id = id ?: "$userId-$createUserId-$refId-$replyId",
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = content,
    type = type,
    userRead = userRead,
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun Notification.toDomain() = NotificationDomain(
    id = id,
    userId = userId,
    createUserId = createUserId,
    refId = refId,
    replyId = replyId,
    content = null,
    type = type,
    userRead = userRead,
    createdOn = createdOn,
    updatedOn = updatedOn
)

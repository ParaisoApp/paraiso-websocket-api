package com.paraiso.domain.notifications

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName(ID) val id: String?,
    val userId: String,
    val replyPostId: String
)

data class NotificationResponse(
    val id: String?

)

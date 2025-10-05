package com.paraiso.domain.notifications

class NotificationsApi(
    private val notificationsDB: NotificationsDB
) {

    suspend fun findByUserId(userId: String) =
        notificationsDB.findByUserId(userId).map { it.toResponse() }

    suspend fun save(notifications: List<NotificationResponse>) =
        notificationsDB.save(notifications.map { it.toDomain() })
    suspend fun setNotificationsRead(
        userId: String,
        type: NotificationType,
        refId: String?
    ) =
        notificationsDB.setNotificationsRead(userId, type, refId)
}

package com.paraiso.domain.notifications

interface NotificationsDB {
    suspend fun findByUserId(userId: String): List<Pair<Notification, String?>>
    suspend fun save(notifications: List<Notification>): Int
    suspend fun setNotificationsRead(
        userId: String,
        type: String,
        refId: String?
    ): Long
}

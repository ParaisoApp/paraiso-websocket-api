package com.paraiso.domain.notifications

import com.paraiso.domain.posts.PostsApi

class NotificationsApi(
    private val notificationsDB: NotificationsDB,
    private val postsApi: PostsApi
) {

    suspend fun findByUserId(userId: String) =
        notificationsDB.findByUserId(userId).let { notifications ->
            // map notification to basic reference post
            val posts = postsApi.getByIdsBasic(userId, notifications.mapNotNull { it.second }.toSet())
            notifications.map { it.first.copy(content = PostNotificationContent(posts[it.second])) }
        }
    suspend fun save(notifications: List<Notification>) =
        notificationsDB.save(notifications)
    suspend fun setNotificationsRead(
        userId: String,
        type: String,
        refId: String?
    ) =
        notificationsDB.setNotificationsRead(userId, type, refId)
}

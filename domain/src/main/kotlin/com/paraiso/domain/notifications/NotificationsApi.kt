package com.paraiso.domain.notifications

import com.paraiso.domain.posts.PostsApi

class NotificationsApi(
    private val notificationsDB: NotificationsDB,
    private val postsApi: PostsApi
) {

    suspend fun findByUserId(userId: String) =
        notificationsDB.findByUserId(userId).let { notifications ->
            //map notification to basic reference post
            val posts = postsApi.getByIdsBasic(userId, notifications.mapNotNull { it.refId }.toSet())
            notifications.map { it.toResponse(posts[it.refId]) }
        }


    suspend fun <T> save(notifications: List<NotificationResponse<T>>): Int {
        val typedNotifs = notifications.map {notification ->
            when(notification.type){
                NotificationType.POST -> {
                    notification.toDomain(null)
                }
                else -> notification.toDomain(notification.content.toString())
            }
        }
        return notificationsDB.save(typedNotifs)
    }
    suspend fun setNotificationsRead(
        userId: String,
        type: String,
        refId: String?
    ) =
        notificationsDB.setNotificationsRead(userId, type, refId)
}

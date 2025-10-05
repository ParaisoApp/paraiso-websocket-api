package com.paraiso.database.notifications

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.notifications.Notification
import com.paraiso.domain.notifications.NotificationType
import com.paraiso.domain.notifications.NotificationsDB
import com.paraiso.domain.users.User
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.votes.Vote
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class NotificationsDBImpl(database: MongoDatabase) : NotificationsDB {

    private val collection = database.getCollection("notifications", Notification::class.java)

    override suspend fun findByUserId(userId: String) =
        collection.find(
            or(
                eq(Notification::userId.name, userId),
                eq(Notification::createUserId.name, userId),
            )
        ).toList()

    override suspend fun save(notifications: List<Notification>): Int {
        val bulkOps = notifications.map { notification ->
            ReplaceOneModel(
                eq(ID, notification.id),
                notification,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun setNotificationsRead(
        userId: String,
        type: NotificationType,
        refId: String?
    ): Long {
        val condList = mutableListOf(
            or(
                eq(Notification::userId.name, userId),
                eq(Notification::createUserId.name, userId),
            ),
            eq(Notification::type.name, type)
        )
        if(refId != null){
            condList.add(
                eq(Notification::refId.name, refId)
            )
        }
        return collection.updateOne(
            and(condList),
            combine(
                set(userId, true),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount
    }
}

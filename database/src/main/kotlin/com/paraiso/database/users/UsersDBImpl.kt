package com.paraiso.database.users

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserFavorite
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UsersDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.bson.Document
import java.util.Date

class UsersDBImpl(database: MongoDatabase) : UsersDB {
    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }

    private val collection = database.getCollection("users", User::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()
    override suspend fun findByIdIn(ids: List<String>) =
        collection.find(`in`(ID, ids)).toList()

    override suspend fun findByName(name: String) =
        collection.find(eq(User::name.name, name)).firstOrNull()

    override suspend fun existsByName(name: String) =
        collection.find(eq(User::name.name, name))
            .limit(1)
            .firstOrNull() != null

    override suspend fun findByPartial(partial: String) =
        collection.find(regex(User::name.name, partial, "i"))
            .limit(PARTIAL_RETRIEVE_LIM)
            .toList()

    override suspend fun getUserList(
        userIds: List<String>,
        filters: FilterTypes,
        followingList: List<String>
    ) =
        collection.find(
            and(
                `in`(ID, userIds),
                or(
                    `in`(User::roles.name, filters.userRoles),
                    and(
                        `in`(User::roles.name, UserRole.FOLLOWING),
                        `in`(User::id.name, followingList)
                    )
                )
            )
        ).toList()

    override suspend fun save(users: List<User>): Int {
        val bulkOps = users.map { user ->
            ReplaceOneModel(
                eq(ID, user.id),
                user,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun addMentions(id: String) =
        collection.findOneAndUpdate(
            eq(ID, id),
            combine(
                inc(User::replies.name, 1),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.id
    override suspend fun addMentionsByName(name: String) =
        collection.findOneAndUpdate(
            eq(User::name.name, name),
            combine(
                inc(User::replies.name, 1),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.id
    override suspend fun setSettings(id: String, settings: UserSettings) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::settings.name, settings),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun markNotifsRead(
        id: String
    ) = coroutineScope {
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::replies.name, 0),
                set(User::chats.name, 0),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        )
    }.modifiedCount

    override suspend fun markReportsRead(
        id: String
    ) = coroutineScope {
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::reports.name, 0),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        )
    }.modifiedCount

    override suspend fun addReport() =
        collection.updateMany(
            `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
            combine(
                inc(User::reports.name, 1),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun setFollowers(
        id: String,
        count: Int
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                inc(User::followers.name, count),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun setFollowing(
        id: String,
        count: Int
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                Updates.inc(User::following.name, count),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun addFavoriteRoute(
        id: String,
        route: String,
        routeFavorite: UserFavorite
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set("${User::routeFavorites.name}.$route", routeFavorite),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun removeFavoriteRoute(
        id: String,
        route: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                Document("\$unset", Document("routeFavorites.$route", "")),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount
    override suspend fun setScore(
        id: String,
        score: Int
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                inc(User::score.name, score),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun addChat(
        id: String,
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                inc(User::chats.name, 1),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun setUserTag(
        tag: Tag
    ) =
        collection.updateOne(
            eq(ID, tag.userId),
            combine(
                set(User::tag.name, tag.tag),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun setUserBanned(
        ban: Ban
    ) =
        collection.updateOne(
            eq(ID, ban.userId),
            combine(
                set(User::banned.name, true),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount
}

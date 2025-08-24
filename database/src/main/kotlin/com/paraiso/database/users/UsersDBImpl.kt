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
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.users.ChatRef
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

class UsersDBImpl(database: MongoDatabase) : UsersDB {
    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }

    private val collection = database.getCollection("users", User::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()

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

    override suspend fun getFollowingById(id: String) =
        collection.find(eq(User::following.name, id)).toList()

    override suspend fun getFollowersById(id: String) =
        collection.find(eq(User::followers.name, id)).toList()

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

    override suspend fun addMentions(id: String, replyId: String) =
        collection.findOneAndUpdate(
            eq(ID, id),
            combine(
                set("${User::replies.name}.$replyId", false),
                set(User::updatedOn.name, Clock.System.now())
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.id
    override suspend fun addMentionsByName(name: String, replyId: String) =
        collection.findOneAndUpdate(
            eq(User::name.name, name),
            combine(
                set("${User::replies.name}.$replyId", false),
                set(User::updatedOn.name, Clock.System.now())
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.id
    override suspend fun setSettings(id: String, settings: UserSettings) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::settings.name, settings),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun markNotifsRead(
        id: String,
        chats: Set<String>,
        replies: Set<String>
    ) = coroutineScope {
        val chatUpdates = chats.map { id ->
            set("${User::chats.name}.$id.viewed", true)
        }
        val replyUpdates = replies.map { id ->
            set("${User::replies.name}.$id", true)
        }
        collection.updateOne(
            eq(ID, id),
            combine(
                chatUpdates +
                    replyUpdates +
                    set(User::updatedOn.name, Clock.System.now())
            )
        )
    }.modifiedCount

    override suspend fun markReportNotifsRead(
        id: String,
        userReports: Set<String>,
        postReports: Set<String>
    ) = coroutineScope {
        val userReportUpdates = userReports.map { id ->
            set("${User::userReports.name}.$id", true)
        }
        val postReportUpdates = postReports.map { id ->
            set("${User::postReports.name}.$id", true)
        }
        collection.updateOne(
            eq(ID, id),
            combine(
                userReportUpdates +
                    postReportUpdates +
                    set(User::updatedOn.name, Clock.System.now())
            )
        )
    }.modifiedCount

    override suspend fun addUserReport(
        id: String
    ) =
        collection.updateMany(
            `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
            combine(
                set("${User::userReports.name}.$id", false),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun addPostReport(
        id: String
    ) =
        collection.updateOne(
            `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
            combine(
                set("${User::postReports.name}.$id", false),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun addFollowers(
        id: String,
        followerUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::followers.name, followerUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun removeFollowers(
        id: String,
        followerUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::followers.name, followerUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun addFollowing(
        id: String,
        followingUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::following.name, followingUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun removeFollowing(
        id: String,
        followingUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::following.name, followingUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun addToBlocklist(
        id: String,
        blockUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::blockList.name, blockUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun removeFromBlocklist(
        id: String,
        blockUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::blockList.name, blockUserId),
                set(User::updatedOn.name, Clock.System.now())
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
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun removeFavoriteRoute(
        id: String,
        routeFavorite: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::routeFavorites.name, routeFavorite),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun addPost(
        id: String,
        postId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::posts.name, postId),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun setUserChat(
        id: String,
        chatId: String,
        chatRef: ChatRef
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set("${User::chats.name}.$chatId", chatRef),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun setUserTag(
        tag: Tag
    ) =
        collection.updateOne(
            eq(ID, tag.userId),
            combine(
                set(User::tag.name, tag.tag),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun setUserBanned(
        ban: Ban
    ) =
        collection.updateOne(
            eq(ID, ban.userId),
            combine(
                set(User::banned.name, true),
                set(User::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount
}

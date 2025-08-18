package com.paraiso.database.users

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag
import com.paraiso.domain.users.ChatRef
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersDBAdapter
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class UsersDBAdapterImpl(database: MongoDatabase) : UsersDBAdapter {
    private val collection = database.getCollection("users", User::class.java)

    suspend fun findById(id: String) =
        collection.find(eq(ID, id)).firstOrNull()

    suspend fun findByName(name: String) =
        collection.find(eq(User::name.name, name)).firstOrNull()

    suspend fun existsByName(name: String): Boolean =
        collection.find(eq(User::name.name, name))
            .limit(1)
            .firstOrNull() != null

    suspend fun findByPartial(partial: String): FindFlow<User> =
        collection.find(regex(User::name.name, partial, "i"))

    fun getUserList(filters: FilterTypes, followingList: Set<String>) =
        collection.find(
            and(
                ne(User::status.name, UserStatus.DISCONNECTED),
                or(
                    `in`(User::roles.name, filters.userRoles),
                    and(
                        `in`(User::roles.name, UserRole.FOLLOWING),
                        `in`(User::id.name, followingList)
                    )
                )
            )
        )

    fun getFollowingById(id: String) =
        collection.find(eq(User::following.name, id))

    fun getFollowersById(id: String) =
        collection.find(eq(User::followers.name, id))

    suspend fun save(users: List<User>) =
        collection.insertMany(users)

    suspend fun setMentions(id: String, replyId: String) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set("${User::replies.name}.$replyId", false),
                set(User::updatedOn.name, Clock.System.now())
            )
        )
    suspend fun setSettings(id: String, settings: UserSettings) =
        collection.updateOne(
            eq(ID, id),
            combine(
                set(User::settings.name, settings),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun markNotifsRead(
        id: String,
        chats: Map<String, ChatRef>,
        replies: Map<String, Boolean>
    ) = coroutineScope {
        val chatUpdates = chats.map { (k, v) ->
            set("${User::chats.name}.$k", v)
        }
        val replyUpdates = replies.map { (k, v) ->
            set("${User::replies.name}.$k", v)
        }
        collection.updateOne(
            eq(ID, id),
            combine(
                chatUpdates +
                    replyUpdates +
                    set(User::updatedOn.name, Clock.System.now())
            )
        )
    }

    suspend fun markReportNotifsRead(
        id: String,
        userReports: Map<String, Boolean>,
        postReports: Map<String, Boolean>
    ) = coroutineScope {
        val userReportUpdates = userReports.map { (k, v) ->
            set("${User::userReports.name}.$k", v)
        }
        val postReportUpdates = postReports.map { (k, v) ->
            set("${User::postReports.name}.$k", v)
        }
        collection.updateOne(
            eq(ID, id),
            combine(
                userReportUpdates +
                    postReportUpdates +
                    set(User::updatedOn.name, Clock.System.now())
            )
        )
    }

    suspend fun addUserReport(
        id: String,
    ) =
        collection.updateOne(
            `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
            combine(
                addToSet(User::userReports.name, id),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addPostReport(
        id: String,
    ) =
        collection.updateOne(
            `in`(User::roles.name, listOf(UserRole.ADMIN, UserRole.MOD)),
            combine(
                addToSet(User::postReports.name, id),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addFollowers(
        id: String,
        followerUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::followers.name, followerUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun removeFollowers(
        id: String,
        followerUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::followers.name, followerUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addFollowing(
        id: String,
        followingUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::following.name, followingUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun removeFollowing(
        id: String,
        followingUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::following.name, followingUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addToBlocklist(
        id: String,
        blockUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::blockList.name, blockUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun removeFromBlocklist(
        id: String,
        blockUserId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::blockList.name, blockUserId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addFavoriteRoute(
        id: String,
        routeFavorite: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::routeFavorites.name, routeFavorite),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun removeFavoriteRoute(
        id: String,
        routeFavorite: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                pull(User::routeFavorites.name, routeFavorite),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun addPost(
        id: String,
        postId: String
    ) =
        collection.updateOne(
            eq(ID, id),
            combine(
                addToSet(User::posts.name, postId),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setUserChat(
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
        )

    suspend fun setUserTag(
        tag: Tag
    ) =
        collection.updateOne(
            eq(ID, tag.userId),
            combine(
                set(User::tag.name, tag.tag),
                set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setUserBanned(
        ban: Ban
    ) =
        collection.updateOne(
            eq(ID, ban.userId),
            combine(
                set(User::banned.name, true),
                set(User::updatedOn.name, Clock.System.now())
            )
        )
}

package com.paraiso.database.users

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.users.ChatRef
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.users.UsersDBAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toSet
import kotlinx.datetime.Clock

class UsersDBAdapterImpl(database: MongoDatabase): UsersDBAdapter {
    private val collection = database.getCollection("users", User::class.java)

    suspend fun findById(id: String) =
        collection.find(eq(User::id.name, id)).firstOrNull()

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
                        `in`(User::id.name, followingList),
                    )
                )
            )
        )

    fun getFollowingById(userId: String) =
        collection.find(eq(User::following.name, userId))

    fun getFollowersById(userId: String) =
        collection.find(eq(User::followers.name, userId))

    suspend fun save(users: List<User>) =
        collection.insertMany(users)

    suspend fun setMentions(userId: String, replies: Map<String, Boolean>) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::replies.name, replies),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )
    suspend fun setSettings(userId: String, settings: UserSettings) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::settings.name, settings),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun markNotifsRead(
        userId: String,
        chats: Map<String, ChatRef>,
        replies: Map<String, Boolean>
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::chats.name, chats),
                Updates.set(User::replies.name, replies),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun markReportNotifsRead(
        userId: String,
        userReports: Map<String, Boolean>,
        postReports: Map<String, Boolean>
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::userReports.name, userReports),
                Updates.set(User::postReports.name, postReports),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setFollowers(
        userId: String,
        followers: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::followers.name, followers),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setFollowing(
        userId: String,
        following: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::following.name, following),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setBlocklist(
        userId: String,
        blockList: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::blockList.name, blockList),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setUserChat(
        userId: String,
        chat: MutableMap<String, ChatRef>,
    ) =
        collection.updateOne(
            eq(User::id.name, userId),
            Updates.combine(
                Updates.set(User::chats.name, chat),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )
}
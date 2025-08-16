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

    fun getFollowingById(id: String) =
        collection.find(eq(User::following.name, id))

    fun getFollowersById(id: String) =
        collection.find(eq(User::followers.name, id))

    suspend fun save(users: List<User>) =
        collection.insertMany(users)

    suspend fun setMentions(id: String, replies: Map<String, Boolean>) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::replies.name, replies),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )
    suspend fun setSettings(id: String, settings: UserSettings) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::settings.name, settings),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun markNotifsRead(
        id: String,
        chats: Map<String, ChatRef>,
        replies: Map<String, Boolean>
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::chats.name, chats),
                Updates.set(User::replies.name, replies),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun markReportNotifsRead(
        id: String,
        userReports: Map<String, Boolean>,
        postReports: Map<String, Boolean>
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::userReports.name, userReports),
                Updates.set(User::postReports.name, postReports),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setFollowers(
        id: String,
        followers: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::followers.name, followers),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setFollowing(
        id: String,
        following: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::following.name, following),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setBlocklist(
        id: String,
        blockList: Set<String>
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::blockList.name, blockList),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun setUserChat(
        id: String,
        chat: MutableMap<String, ChatRef>,
    ) =
        collection.updateOne(
            eq(User::id.name, id),
            Updates.combine(
                Updates.set(User::chats.name, chat),
                Updates.set(User::updatedOn.name, Clock.System.now())
            )
        )
}
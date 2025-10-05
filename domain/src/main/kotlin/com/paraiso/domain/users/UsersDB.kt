package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag

interface UsersDB {
    suspend fun findById(id: String): User?
    suspend fun findByIdIn(ids: List<String>): List<User>
    suspend fun findByName(name: String): User?
    suspend fun existsByName(name: String): Boolean
    suspend fun findByPartial(partial: String): List<User>
    suspend fun getUserList(
        userIds: List<String>,
        filters: FilterTypes,
        followingList: List<String>
    ): List<User>
    suspend fun save(users: List<User>): Int
    suspend fun addMentions(id: String, replyId: String): String?
    suspend fun addMentionsByName(name: String, replyId: String): String?
    suspend fun setSettings(id: String, settings: UserSettings): Long
    suspend fun markNotifsRead(
        id: String,
        chats: Set<String>,
        replies: Set<String>
    ): Long
    suspend fun markReportNotifsRead(
        id: String,
        userReports: Set<String>,
        postReports: Set<String>
    ): Long
    suspend fun addUserReport(
        id: String
    ): Long
    suspend fun addPostReport(
        id: String
    ): Long
    suspend fun setFollowers(
        id: String,
        count: Int
    ): Long
    suspend fun setFollowing(
        id: String,
        count: Int
    ): Long
    suspend fun addToBlocklist(
        id: String,
        blockUserId: String
    ): Long
    suspend fun removeFromBlocklist(
        id: String,
        blockUserId: String
    ): Long
    suspend fun addFavoriteRoute(
        id: String,
        route: String,
        routeFavorite: UserFavorite
    ): Long
    suspend fun removeFavoriteRoute(
        id: String,
        route: String
    ): Long
    suspend fun setScore(
        id: String,
        score: Int
    ): Long
    suspend fun setUserChat(
        id: String,
        chatId: String,
        chatRef: ChatRef
    ): Long
    suspend fun setUserTag(
        tag: Tag
    ): Long
    suspend fun setUserBanned(
        ban: Ban
    ): Long
}

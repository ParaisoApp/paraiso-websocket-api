package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag

interface UsersDBAdapter{
    suspend fun findById(id: String): User?
    suspend fun findByName(name: String): User?
    suspend fun existsByName(name: String): Boolean
    suspend fun findByPartial(partial: String): List<User>
    suspend fun getUserList(
        filters: FilterTypes,
        followingList: List<String>
    ): List<User>
    suspend fun getFollowingById(id: String): List<User>
    suspend fun getFollowersById(id: String): List<User>
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
    suspend fun addFollowers(
        id: String,
        followerUserId: String
    ): Long
    suspend fun removeFollowers(
        id: String,
        followerUserId: String
    ): Long
    suspend fun addFollowing(
        id: String,
        followingUserId: String
    ): Long
    suspend fun removeFollowing(
        id: String,
        followingUserId: String
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
        routeFavorite: String
    ): Long
    suspend fun addPost(
        id: String,
        postId: String
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

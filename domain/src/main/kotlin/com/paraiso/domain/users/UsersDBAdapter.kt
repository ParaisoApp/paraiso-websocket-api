package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.Ban
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Tag

interface UsersDBAdapter{
    suspend fun findById(id: String): User?
    suspend fun findByName(name: String): User?
    suspend fun existsByName(name: String): Boolean
    suspend fun findByPartial(partial: String): List<User>
    suspend fun getUserList(filters: FilterTypes, followingList: Set<String>): List<User>
    suspend fun getFollowingById(id: String): List<User>
    suspend fun getFollowersById(id: String): List<User>
    suspend fun save(users: List<User>): Int
    suspend fun setMentions(id: String, replyId: String): Long
    suspend fun setSettings(id: String, settings: UserSettings): Long
    suspend fun markNotifsRead(
        id: String,
        chats: Map<String, ChatRef>,
        replies: Map<String, Boolean>
    ): Long
    suspend fun markReportNotifsRead(
        id: String,
        userReports: Map<String, Boolean>,
        postReports: Map<String, Boolean>
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
        routeFavorite: String
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

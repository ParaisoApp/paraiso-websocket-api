package com.paraiso.domain.userchats

interface UserChatsDB {
    suspend fun findByUserIds(userId: String, otherUserId: String): UserChat?
    suspend fun findByUserId(userId: String): List<UserChat>
    suspend fun save(chats: List<UserChat>): Int
    suspend fun setMostRecentDm(
        dmId: String,
        chatId: String
    ): Long
}

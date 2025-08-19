package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage

interface UserChatsDBAdapter {
    suspend fun findById(id: String): UserChat?
    suspend fun save(chats: List<UserChat>): Int
    suspend fun addToUserChat(
        chatId: String,
        dm: DirectMessage
    ): Long
}

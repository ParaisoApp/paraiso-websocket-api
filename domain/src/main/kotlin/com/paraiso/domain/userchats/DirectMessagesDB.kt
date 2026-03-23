package com.paraiso.domain.userchats

interface DirectMessagesDB {
    suspend fun findByIdIn(ids: List<String>): List<DirectMessage>
    suspend fun findByChatId(chatId: String, userId: String): List<DirectMessage>
    suspend fun save(dms: List<DirectMessage>): Int
}

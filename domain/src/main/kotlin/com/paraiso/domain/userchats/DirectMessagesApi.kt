package com.paraiso.domain.userchats

class DirectMessagesApi(
    private val directMessagesDB: DirectMessagesDB
) {

    suspend fun findByIdIn(ids: List<String>) =
        directMessagesDB.findByIdIn(ids)
    suspend fun findByChatId(chatId: String, userId: String) =
        directMessagesDB.findByChatId(chatId, userId)
    suspend fun save(dm: DirectMessage) =
        directMessagesDB.save(listOf(dm))
}

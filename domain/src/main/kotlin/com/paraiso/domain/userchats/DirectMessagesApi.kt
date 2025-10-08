package com.paraiso.domain.userchats

class DirectMessagesApi(
    private val directMessagesDB: DirectMessagesDB
) {

    suspend fun findByIdIn(ids: List<String>) =
        directMessagesDB.findByIdIn(ids).map { it.toResponse() }
    suspend fun findByChatId(chatId: String) =
        directMessagesDB.findByChatId(chatId).map { it.toResponse() }
    suspend fun save(dm: DirectMessageResponse) =
        directMessagesDB.save(listOf(dm.toDomain()))
}

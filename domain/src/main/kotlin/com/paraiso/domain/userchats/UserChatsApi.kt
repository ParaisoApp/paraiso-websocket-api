package com.paraiso.domain.userchats

import kotlinx.datetime.Clock
import java.util.UUID

class UserChatsApi(
    private val userChatsDB: UserChatsDB,
    private val directMessagesApi: DirectMessagesApi
) {

    suspend fun getOrPutUserChat(userId: String, otherUserId: String): UserChatResponse {
        val chat = userChatsDB.findByUserIds(userId, otherUserId) ?: run {
            val now = Clock.System.now()
            UUID.randomUUID().toString().let { newChatId ->
                val newUserChat = UserChat(
                    id = newChatId,
                    userIds = setOf(userId, otherUserId),
                    recentDm = null,
                    createdOn = now,
                    updatedOn = now
                )
                userChatsDB.save(listOf(newUserChat))
                newUserChat
            }
        }
        val dms = directMessagesApi.findByChatId(chat.id)
        return chat.toResponse(dms)
    }

    suspend fun findByUserId(userId: String) =
        userChatsDB.findByUserId(userId).let { userChats ->
            val recentDms = directMessagesApi.findByIdIn(userChats.mapNotNull{ it.recentDm }).associateBy {
                it.id
            }
            userChats.map {
                val dms = recentDms[it.recentDm]?.let{recentDm ->
                    listOf(recentDm)
                } ?: emptyList()
                it.toResponse(dms)
            }
        }

    suspend fun setMostRecentDm(dmId: String, chatId: String) =
        userChatsDB.setMostRecentDm(dmId, chatId)
}

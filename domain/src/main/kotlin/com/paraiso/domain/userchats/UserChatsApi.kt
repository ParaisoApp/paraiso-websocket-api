package com.paraiso.domain.userchats

import kotlinx.datetime.Clock
import java.util.UUID

class UserChatsApi(
    private val userChatsDB: UserChatsDB,
    private val directMessagesApi: DirectMessagesApi
) {

    suspend fun getOrPutUserChat(userId: String, otherUserId: String): UserChat {
        val chat = userChatsDB.findByUserIds(userId, otherUserId)
        if(chat != null){
            val dms = directMessagesApi.findByChatId(chat.id, userId)
            return chat.copy(dms = dms)
        }else{
            val now = Clock.System.now()
            UUID.randomUUID().toString().let { newChatId ->
                val newUserChat = UserChat(
                    id = newChatId,
                    userIds = mapOf(userId to true, otherUserId to true),
                    dms = emptyList(),
                    createdOn = now,
                    updatedOn = now
                )
                userChatsDB.save(listOf(newUserChat))
                return newUserChat
            }
        }
    }

    suspend fun findByUserId(userId: String) =
        userChatsDB.findByUserId(userId).let { userChats ->
            val recentDms = directMessagesApi.findByIdIn(userChats.mapNotNull { it.second }).associateBy {
                it.id
            }
            userChats.map {
                val dms = recentDms[it.second]?.let { recentDm ->
                    listOf(recentDm)
                } ?: emptyList()
                it.first.copy(dms = dms)
            }
        }

    suspend fun setMostRecentDm(dmId: String, chatId: String) =
        userChatsDB.setMostRecentDm(dmId, chatId)
}

package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import java.util.UUID

class UserChatsApi(private val userChatsDBAdapter: UserChatsDBAdapter) {

    companion object {
        const val RETRIEVE_LIM = 5
    }

    suspend fun getOrPutUserChat(chatId: String, userId: String, otherUserId: String) =
        (userChatsDBAdapter.findById(chatId) ?: run {
            val now = Clock.System.now()
            UUID.randomUUID().toString().let { newChatId ->
                val newUserChat = UserChat(
                    id = newChatId,
                    userIds = setOf(userId, otherUserId),
                    dms = emptySet(),
                    createdOn = now,
                    updatedOn = now
                )
                userChatsDBAdapter.save(listOf(newUserChat))
                newUserChat
            }
        }).toReturn()

    suspend fun putDM(dm: DirectMessage) = coroutineScope {
        userChatsDBAdapter.addToUserChat(dm.chatId, dm)
    }
}

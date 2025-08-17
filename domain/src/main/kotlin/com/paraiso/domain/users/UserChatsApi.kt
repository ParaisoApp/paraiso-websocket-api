package com.paraiso.domain.users

import com.paraiso.domain.messageTypes.DirectMessage
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class UserChatsApi {

    companion object {
        const val RETRIEVE_LIM = 5
    }

    fun getOrPutUserChat(chatId: String, userId: String, otherUserId: String) =
        ServerState.userChatList[chatId]?.toReturn() ?: run {
            Clock.System.now().let { now ->
                UUID.randomUUID().toString().let { newChatId ->
                    val newUserChat = UserChat(
                        id = newChatId,
                        userIds = setOf(userId, otherUserId),
                        dms = emptySet(),
                        createdOn = now,
                        updatedOn = now
                    )
                    ServerState.userChatList[newChatId] = newUserChat
                    newUserChat.toReturn()
                }
            }
        }

    suspend fun putDM(dm: DirectMessage, now: Instant) = coroutineScope {
        ServerState.userChatList[dm.chatId]?.let { chat ->
            ServerState.userChatList[dm.chatId] = chat.copy(
                dms = chat.dms + dm.copy(
                    createdOn = now
                ),
                updatedOn = now
            )
        }
    }
}

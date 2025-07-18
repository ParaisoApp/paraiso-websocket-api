package com.paraiso.com.paraiso.server.plugins

import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class ServerHandler {
    suspend fun cleanUserList() = coroutineScope {
        while (isActive) {
            ServerState.userList.entries
                .removeIf {
                    it.value.lastSeen != 0L && // clear user from user list if disconnected for more than 10 min
                        it.value.lastSeen < System.currentTimeMillis() - (10 * 60 * 1000) &&
                        it.value.status == UserStatus.DISCONNECTED
                }
            delay(10 * 60 * 1000) // delay for ten minutes
        }
    }
}

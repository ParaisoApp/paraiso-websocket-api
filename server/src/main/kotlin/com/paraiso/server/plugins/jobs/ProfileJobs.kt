package com.paraiso.server.plugins.jobs

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ProfileJobs {
    suspend fun profileJobs(content: String?, session: WebSocketServerSession) = coroutineScope {
        listOf(launch {})
    }
}

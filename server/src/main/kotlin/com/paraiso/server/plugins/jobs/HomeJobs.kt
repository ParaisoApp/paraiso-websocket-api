package com.paraiso.com.paraiso.server.plugins.jobs

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeJobs {
    suspend fun homeJobs(session: WebSocketServerSession) = coroutineScope {
        listOf(launch {})
    }
}

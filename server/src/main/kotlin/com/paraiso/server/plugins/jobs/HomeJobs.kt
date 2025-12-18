package com.paraiso.server.plugins.jobs

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeJobs {
    suspend fun homeJobs(session: WebSocketServerSession) = withContext(Dispatchers.IO) {
        listOf(launch {})
    }
}

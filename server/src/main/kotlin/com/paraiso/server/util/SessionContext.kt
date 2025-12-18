package com.paraiso.server.util

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.init
import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.channels.Channel
import java.util.UUID

data class SessionContext(
    val session: WebSocketServerSession,
    val sessionId: String = UUID.randomUUID().toString(),
    val inboundChannel: Channel<String> = Channel(Channel.UNLIMITED),
    var filterTypes: FilterTypes = FilterTypes.init()
)

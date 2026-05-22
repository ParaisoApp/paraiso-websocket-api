package com.paraiso.domain.util

import com.paraiso.domain.messageTypes.ServerEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ServerState {
    val eventReceivedFlowMut = MutableSharedFlow<ServerEvent>(replay = 0)
    val eventReceivedFlow = eventReceivedFlowMut.asSharedFlow()
}

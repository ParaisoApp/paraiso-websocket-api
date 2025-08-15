package com.paraiso.domain.routes

import com.paraiso.domain.util.ServerState

class RoutesApi {
    fun getById(id: String) = ServerState.routes[id]?.toReturn()
}

package com.paraiso.domain.routes

import com.paraiso.domain.util.ServerState

class RoutesApi {

    companion object {
        const val RETRIEVE_LIM = 50
        const val PARTIAL_RETRIEVE_LIM = 5
        const val TIME_WEIGHTING = 10000000000
    }

    fun getById(id: String) = ServerState.routes[id]

}

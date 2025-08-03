package com.paraiso.domain.admin

import com.paraiso.domain.util.ServerState

class AdminApi {

    companion object {
        const val PARTIAL_RETRIEVE_LIM = 5
    }
    fun getUserReports() =
        ServerState.userReports

    fun getPostReports() =
        ServerState.postReports

}

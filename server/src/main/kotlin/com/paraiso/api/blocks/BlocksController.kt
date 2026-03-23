package com.paraiso.api.blocks

import com.paraiso.api.util.UserCookie
import com.paraiso.domain.blocks.BlockResponse
import com.paraiso.domain.blocks.BlocksApi
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.blocksController(blocksApi: BlocksApi) {
    route("blocks") {
        post {
            blocksApi.block(
                BlockResponse(
                    blockerId = call.sessions.get<UserCookie>()?.userId ?: "",
                    blockeeId = call.request.queryParameters["blockeeId"] ?: "",
                    blocking = call.request.queryParameters["blocking"]?.toBoolean() == true
                )
            )
        }
    }
}

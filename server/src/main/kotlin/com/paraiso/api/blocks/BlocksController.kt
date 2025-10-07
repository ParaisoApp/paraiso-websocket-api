package com.paraiso.api.blocks

import com.paraiso.domain.blocks.BlockResponse
import com.paraiso.domain.blocks.BlocksApi
import com.paraiso.domain.follows.FollowsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.blocksController(blocksApi: BlocksApi) {
    route("blocks") {
        post {
            blocksApi.block(
                BlockResponse(
                    blockerId = call.request.queryParameters["blockerId"] ?: "",
                    blockeeId = call.request.queryParameters["blockeeId"] ?: "",
                    blocking = call.request.queryParameters["blocking"]?.toBoolean() == true
                )
            )
        }
    }
}

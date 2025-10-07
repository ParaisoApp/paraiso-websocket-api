package com.paraiso.api.follows

import com.paraiso.domain.follows.FollowsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.followsController(followsApi: FollowsApi) {
    route("follows") {
        get {
            followsApi.findIn(
                call.request.queryParameters["followerId"] ?: "",
                listOf(call.request.queryParameters["followeeId"] ?: "")
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

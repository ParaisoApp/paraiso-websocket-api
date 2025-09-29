package com.paraiso.api.votes

import com.paraiso.domain.votes.VotesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get

fun Route.votesController(votesApi: VotesApi) {
    route("votes") {
        get {
            votesApi.get(
                call.request.queryParameters["userId"] ?: "",
                call.request.queryParameters["postId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

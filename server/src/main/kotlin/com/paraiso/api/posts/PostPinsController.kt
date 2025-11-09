package com.paraiso.api.posts

import com.paraiso.domain.posts.PostPinsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route


fun Route.postPinsController(postPinsApi: PostPinsApi) {
    route("postPins") {
        post {
            postPinsApi.save(
                call.request.queryParameters["routeId"] ?: "",
                call.request.queryParameters["postId"] ?: "",
                call.request.queryParameters["userId"] ?: "",
                call.request.queryParameters["order"]?.toIntOrNull() ?: 0
            ).let {
                call.respond(HttpStatusCode.OK)
            }
        }
        delete {
            postPinsApi.delete(
                call.request.queryParameters["id"] ?: "",
            ).let {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
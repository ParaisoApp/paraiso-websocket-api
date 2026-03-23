package com.paraiso.api.posts

import com.paraiso.api.util.UserCookie
import com.paraiso.api.util.isElevated
import com.paraiso.api.util.withElevatedAuth
import com.paraiso.api.util.withElevatedAuthRoute
import com.paraiso.domain.posts.PostPinsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.postPinsController(postPinsApi: PostPinsApi) {
    route("postPins") {
        post {
            val routeId = call.request.queryParameters["routeId"] ?: ""
            withElevatedAuthRoute(routeId){ userId ->
                postPinsApi.save(
                    routeId,
                    call.request.queryParameters["postId"] ?: "",
                    userId,
                    call.request.queryParameters["order"]?.toIntOrNull() ?: 0
                ).let {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
        delete {
            val routeId = call.request.queryParameters["routeId"] ?: ""
            withElevatedAuthRoute(routeId){
                postPinsApi.delete(
                    call.request.queryParameters["id"] ?: ""
                ).let {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

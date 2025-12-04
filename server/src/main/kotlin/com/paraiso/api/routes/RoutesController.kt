package com.paraiso.api.routes

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.Range
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.RoutesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.routesController(routesApi: RoutesApi) {
    route("routes") {
        get {
            routesApi.getById(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["userId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        post {
            routesApi.initPage(
                call.request.queryParameters["routeId"] ?: "",
                call.request.queryParameters["userId"] ?: "",
                call.receive<FilterTypes>(),
                call.request.queryParameters["postId"]
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

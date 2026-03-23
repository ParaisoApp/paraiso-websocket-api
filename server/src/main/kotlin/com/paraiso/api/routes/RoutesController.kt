package com.paraiso.api.routes

import com.paraiso.api.util.UserCookie
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.InitSearch
import com.paraiso.domain.posts.InitSearchRequest
import com.paraiso.domain.posts.toDomain
import com.paraiso.domain.routes.RoutesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.routesController(routesApi: RoutesApi) {
    route("routes") {
        get {
            routesApi.getById(
                call.request.queryParameters["id"] ?: "",
                call.sessions.get<UserCookie>()?.userId ?: "",
                call.request.queryParameters["sessionId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        post {
            routesApi.initPage(
                call.receive<InitSearchRequest>().toDomain(userId = call.sessions.get<UserCookie>()?.userId ?: "")
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

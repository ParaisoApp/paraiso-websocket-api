package com.paraiso.com.paraiso.api.routes

import com.paraiso.domain.routes.RoutesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.routesController(routesApi: RoutesApi) {
    route("routes") {
        get("byId") {
            routesApi.getById(call.request.queryParameters["id"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("byUserName") {
            routesApi.getById(call.request.queryParameters["name"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

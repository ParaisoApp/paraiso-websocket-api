package com.paraiso.api.users

import com.paraiso.domain.users.UsersApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.usersController(usersApi: UsersApi) {
    route("users") {
        post("/markNotifsRead") {
            usersApi.markNotifsRead(
                call.request.queryParameters["id"] ?: ""
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/markReportsRead") {
            usersApi.markReportsRead(
                call.request.queryParameters["id"] ?: ""
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}

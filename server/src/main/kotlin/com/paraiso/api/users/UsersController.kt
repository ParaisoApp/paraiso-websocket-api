package com.paraiso.api.users

import com.paraiso.domain.users.UserResponse
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UsersApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.usersController(usersApi: UsersApi) {
    route("users") {
        post {
            usersApi.saveUser(
                call.receive<UserResponse>()
            )
        }
        post("/settings") {
            usersApi.setSettings(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserSettings>()
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/settings") {
            usersApi.setSettings(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserSettings>()
            )
            call.respond(HttpStatusCode.OK)
        }
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

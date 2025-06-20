package com.paraiso.com.paraiso.api.users

import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.users.UsersApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route


fun Route.usersController(usersApi: UsersApi) {
    route("users") {
        get {
            call.respond(HttpStatusCode.OK, usersApi.getUserList())
        }
        get("/chat") {
            call.respond(
                HttpStatusCode.OK,
                usersApi.getUserChat(call.request.queryParameters["id"] ?: "")
            )
        }
        get("/findByName") {
            usersApi.getUserByName(call.request.queryParameters["userName"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        post {
            usersApi.setSettings(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserSettings>()
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}

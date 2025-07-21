package com.paraiso.com.paraiso.api.users

import com.paraiso.domain.messageTypes.FilterTypes
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
        post {
            usersApi.getUserList(
                call.receive<FilterTypes>(),
                call.request.queryParameters["id"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/chat") {
            call.respond(
                HttpStatusCode.OK,
                usersApi.getUserChat(call.request.queryParameters["id"] ?: "")
            )
        }
        get("/findByName") {
            usersApi.getUserByName(call.request.queryParameters["name"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findById") {
            usersApi.getUserById(call.request.queryParameters["id"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findFollowingById") {
            usersApi.getFollowingById(call.request.queryParameters["id"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/findFollowersById") {
            usersApi.getFollowersById(call.request.queryParameters["id"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("/settings") {
            usersApi.setSettings(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserSettings>()
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}

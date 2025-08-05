package com.paraiso.com.paraiso.api.users

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.users.UserNotifs
import com.paraiso.domain.users.UserReportNotifs
import com.paraiso.domain.users.UserResponse
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
            usersApi.saveUser(
                call.receive<UserResponse>()
            )
        }
        post("/userList") {
            usersApi.getUserList(
                call.receive<FilterTypes>(),
                call.request.queryParameters["id"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/chat") {
            usersApi.getOrPutUserChat(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["userId"] ?: "",
                call.request.queryParameters["otherUserId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/getByName") {
            usersApi.getUserByName(call.request.queryParameters["name"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/getById") {
            usersApi.getUserById(call.request.queryParameters["id"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/getByPartial") {
            usersApi.getUserByPartial(call.request.queryParameters["search"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/getFollowingById") {
            usersApi.getFollowingById(call.request.queryParameters["id"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/getFollowersById") {
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
        post("/settings") {
            usersApi.setSettings(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserSettings>()
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/markNotifsRead") {
            usersApi.markNotifsRead(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserNotifs>(),
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/markReportNotifsRead") {
            usersApi.markReportNotifsRead(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserReportNotifs>(),
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/markReportNotifsRead") {
            usersApi.markReportNotifsRead(
                call.request.queryParameters["id"] ?: "",
                call.receive<UserReportNotifs>(),
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/toggleBlockUser") {
            usersApi.toggleBlockUser(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["blockUserId"] ?: ""
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/toggleFollowRoute") {
            usersApi.toggleFollowRoute(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["route"] ?: ""
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}

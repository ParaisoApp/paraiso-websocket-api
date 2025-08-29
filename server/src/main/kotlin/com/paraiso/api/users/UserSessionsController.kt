package com.paraiso.com.paraiso.api.users

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UserSessionsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userSessionsController(userSessionsApi: UserSessionsApi) {
    route("userSessions") {
        get("/getByName") {
            userSessionsApi.getUserByName(call.request.queryParameters["name"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/getById") {
            userSessionsApi.getUserById(call.request.queryParameters["id"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/getByPartial") {
            userSessionsApi.getUserByPartial(call.request.queryParameters["search"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/exists") {
            userSessionsApi.exists(call.request.queryParameters["search"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/getFollowingById") {
            userSessionsApi.getFollowingById(call.request.queryParameters["id"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/getFollowersById") {
            userSessionsApi.getFollowersById(call.request.queryParameters["id"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("userList") {
            userSessionsApi.getUserList(
                call.receive<FilterTypes>(),
                call.request.queryParameters["id"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

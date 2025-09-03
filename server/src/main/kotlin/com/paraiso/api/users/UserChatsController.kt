package com.paraiso.api.users

import com.paraiso.domain.users.UserChatsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.userChatsController(userChatsApi: UserChatsApi) {
    route("userChats") {
        get {
            userChatsApi.getOrPutUserChat(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["userId"] ?: "",
                call.request.queryParameters["otherUserId"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

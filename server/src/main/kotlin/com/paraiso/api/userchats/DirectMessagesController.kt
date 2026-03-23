package com.paraiso.api.userchats

import com.paraiso.api.util.UserCookie
import com.paraiso.domain.userchats.DirectMessagesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.directMessagesController(directMessagesApi: DirectMessagesApi) {
    route("dms") {
        get {
            directMessagesApi.findByChatId(
                call.request.queryParameters["id"] ?: "",
                call.sessions.get<UserCookie>()?.userId ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

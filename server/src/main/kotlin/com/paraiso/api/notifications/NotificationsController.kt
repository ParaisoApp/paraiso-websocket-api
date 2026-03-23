package com.paraiso.api.notifications

import com.paraiso.api.util.UserCookie
import com.paraiso.domain.notifications.NotificationsApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.notificationsController(notificationsApi: NotificationsApi) {
    route("notifications") {
        get {
            notificationsApi.findByUserId(call.sessions.get<UserCookie>()?.userId ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("markNotifsRead") {
            notificationsApi.setNotificationsRead(
                call.sessions.get<UserCookie>()?.userId ?: "",
                call.request.queryParameters["type"] ?: "",
                call.request.queryParameters["refId"]
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

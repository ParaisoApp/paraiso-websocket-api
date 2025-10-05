package com.paraiso.api.notifications

import com.paraiso.domain.notifications.NotificationsApi
import com.paraiso.domain.votes.VotesApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.notificationsController(notificationsApi: NotificationsApi) {
    route("votes") {
        get {
            notificationsApi.findByUserId(
                call.request.queryParameters["userId"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("markNotificationsRead") {
            notificationsApi.setNotificationsRead(
                call.request.queryParameters["userId"] ?: "",
                call.request.queryParameters["type"] ?: "",
                call.request.queryParameters["refId"],
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

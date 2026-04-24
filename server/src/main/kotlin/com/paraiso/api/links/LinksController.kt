package com.paraiso.api.links

import com.paraiso.domain.links.LinksApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.linksController(linksApi: LinksApi) {
    route("links") {
        get {
            linksApi.findLinksByType(
                call.request.queryParameters["type"] ?: "",
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}
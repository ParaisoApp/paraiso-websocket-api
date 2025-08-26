package com.paraiso.com.paraiso.api.metadata

import com.paraiso.domain.metadata.MetadataApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.metadataController(metadataApi: MetadataApi) {
    route("metadata") {
        get {
            metadataApi.getMetadata(call.request.queryParameters["url"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

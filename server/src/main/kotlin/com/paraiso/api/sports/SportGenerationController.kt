package com.paraiso.api.sports

import com.paraiso.server.plugins.ServerHandler
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.sports.SportHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.dataGenerationController(
    serverHandler: ServerHandler,
    sportHandler: SportHandler
) {
    route("dataGen") {
        post("/rosters") {
            serverHandler.buildRoutes(manual = true)
            call.respond(HttpStatusCode.OK)
        }
        post("/league") {
            sportHandler.buildLeague(
                enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                manual = true
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/teams") {
            sportHandler.buildTeams(
                enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                manual = true
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/schedules") {
            sportHandler.buildSchedules(
                enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                manual = true
            )
            call.respond(HttpStatusCode.OK)
        }
        post("/rosters") {
            sportHandler.buildRosters(
                enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                manual = true
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
package com.paraiso.api.sports

import com.paraiso.api.util.withAdminAuth
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.sports.SportHandler
import com.paraiso.server.plugins.ServerHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.dataGenerationController(
    serverHandler: ServerHandler,
    sportHandler: SportHandler
) {
    authenticate("auth0") {
        route("dataGen") {
            post("/routes") {
                withAdminAuth {
                    serverHandler.buildRoutes(manual = true)
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/league") {
                withAdminAuth {
                    sportHandler.buildLeague(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        manual = true
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/teams") {
                withAdminAuth {
                    sportHandler.buildTeams(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        manual = true
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/schedules") {
                withAdminAuth {
                    sportHandler.buildSchedules(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        manual = true
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/rosters") {
                withAdminAuth {
                    sportHandler.buildRosters(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        manual = true
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/fillCompetitionData") {
                withAdminAuth {
                    sportHandler.fillCompetitionData(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        call.request.queryParameters["dates"] ?: ""
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("/fillPlayoffData") {
                withAdminAuth {
                    sportHandler.fillPlayoffs(
                        enumValueOf<SiteRoute>(call.request.queryParameters["sport"] ?: ""),
                        call.request.queryParameters["year"]?.toIntOrNull() ?: 0
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

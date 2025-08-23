package com.paraiso.com.paraiso.api.sports.bball

import com.paraiso.domain.sport.sports.bball.BBallApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.bballController(bBallApi: BBallApi) {
    route("basketball") {
        get("/teams") {
            call.respond(HttpStatusCode.OK, bBallApi.getTeams())
        }
        get("/standings") {
            bBallApi.getStandings()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leaders") {
            bBallApi.getLeaders(
                call.request.queryParameters["seasonYear"] ?: "",
                call.request.queryParameters["seasonType"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leader_cats") {
            bBallApi.getLeaderCategories()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findTeamByAbbr") {
            bBallApi.getTeamByAbbr(call.request.queryParameters["teamAbbr"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_roster") {
            bBallApi.getTeamRoster(call.request.queryParameters["teamId"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_schedule") {
            bBallApi.getTeamSchedule(
                call.request.queryParameters["teamId"] ?: "",
                call.request.queryParameters["seasonYear"] ?: "",
                call.request.queryParameters["seasonType"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
    }
}

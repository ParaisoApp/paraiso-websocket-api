package com.paraiso.com.paraiso.api.sports

import com.paraiso.domain.sport.SportApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.sportsController(sportApi: SportApi) {
    route("sport") {
        get("/teams") {
            call.respond(HttpStatusCode.OK, sportApi.getTeams())
        }
        get("/standings") {
            sportApi.getStandings()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leaders") {
            sportApi.getLeaders()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leader_cats") {
            sportApi.getLeaderCategories()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findTeamByAbbr") {
            sportApi.getTeamByAbbr(call.request.queryParameters["teamAbbr"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_roster") {
            sportApi.getTeamRoster(call.request.queryParameters["teamId"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_schedule") {
            sportApi.getTeamSchedule(call.request.queryParameters["teamId"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
    }
}

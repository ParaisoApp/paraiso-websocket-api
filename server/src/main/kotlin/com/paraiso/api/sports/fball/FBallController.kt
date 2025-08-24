package com.paraiso.com.paraiso.api.sports.fball

import com.paraiso.domain.sport.sports.fball.FBallApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.fballController(fBallApi: FBallApi) {
    route("football") {
        get("/league") {
            fBallApi.getLeague()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/teams") {
            call.respond(HttpStatusCode.OK, fBallApi.getTeams())
        }
        get("/standings") {
            fBallApi.getStandings()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leaders") {
            fBallApi.getLeaders(
                call.request.queryParameters["seasonYear"] ?: "",
                call.request.queryParameters["seasonType"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leader_cats") {
            fBallApi.getLeaderCategories()?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findTeamByAbbr") {
            fBallApi.getTeamByAbbr(call.request.queryParameters["teamAbbr"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_roster") {
            fBallApi.getTeamRoster(call.request.queryParameters["teamId"] ?: "")?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_schedule") {
            fBallApi.getTeamSchedule(
                call.request.queryParameters["teamId"] ?: "",
                call.request.queryParameters["seasonYear"] ?: "",
                call.request.queryParameters["seasonType"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
    }
}

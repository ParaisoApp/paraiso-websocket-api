package com.paraiso.com.paraiso.api.sports

import com.paraiso.domain.sport.sports.SportApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.sportController(sportApi: SportApi) {
    route("s") {
        get("/leagues") {
            sportApi.getLeague(
                call.request.queryParameters["sport"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/teams") {
            call.respond(HttpStatusCode.OK, sportApi.getTeams(
                call.request.queryParameters["sport"] ?: ""
            ))
        }
        get("/standings") {
            sportApi.getStandings(
                call.request.queryParameters["sport"] ?: ""
                )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leaders") {
            sportApi.getLeaders(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["seasonYear"] ?: "",
                call.request.queryParameters["seasonType"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/leader_cats") {
            sportApi.getLeaderCategories(
                call.request.queryParameters["sport"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/findTeamByAbbr") {
            sportApi.getTeamByAbbr(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamAbbr"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_roster") {
            sportApi.getTeamRoster(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("/team_schedule") {
            sportApi.getTeamSchedule(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamId"] ?: "",
                call.request.queryParameters["seasonYear"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["seasonType"]?.toIntOrNull() ?: 0
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
    }
}

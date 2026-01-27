package com.paraiso.api.sports

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
            sportApi.findLeague(
                call.request.queryParameters["sport"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/teams") {
            call.respond(
                HttpStatusCode.OK,
                sportApi.findTeams(
                    call.request.queryParameters["sport"] ?: ""
                )
            )
        }
        get("/teamById") {
            sportApi.findTeamById(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["id"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/competitionById") {
            sportApi.findCompetitionById(
                call.request.queryParameters["id"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/boxScoresById") {
            sportApi.findBoxScoresById(
                call.request.queryParameters["id"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/standings") {
            sportApi.findStandings(
                call.request.queryParameters["sport"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/leaders") {
            sportApi.findLeaders(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["seasonYear"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["seasonType"]?.toIntOrNull() ?: 0
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/teamByAbbr") {
            sportApi.findTeamByAbbr(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamAbbr"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/teamRoster") {
            sportApi.findTeamRoster(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/teamSchedule") {
            sportApi.findTeamSchedule(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamId"] ?: "",
                call.request.queryParameters["seasonYear"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["seasonType"]?.toIntOrNull() ?: 0
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/teamLeaders") {
            sportApi.findTeamLeaders(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["teamId"] ?: "",
                call.request.queryParameters["seasonYear"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["seasonType"]?.toIntOrNull() ?: 0
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        get("/scoreboard") {
            sportApi.findScoreboard(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["year"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["type"]?.toIntOrNull() ?: 0,
                call.request.queryParameters["modifier"] ?: "",
                call.request.queryParameters["past"].toBoolean()
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("/playoff") {
            sportApi.findPlayoff(
                call.request.queryParameters["sport"] ?: "",
                call.request.queryParameters["year"]?.toIntOrNull() ?: 0,
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
    }
}

package com.paraiso.api.admin

import com.paraiso.domain.admin.AdminApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.adminController(adminApi: AdminApi) {
    authenticate("auth0") {
        route("admin") {
            get("/userReports") {
                adminApi.getUserReports().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
            get("/postReports") {
                adminApi.getPostReports().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
        }
    }
}

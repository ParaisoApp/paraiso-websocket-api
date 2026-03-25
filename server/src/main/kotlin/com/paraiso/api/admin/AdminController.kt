package com.paraiso.api.admin

import com.paraiso.api.util.UserCookie
import com.paraiso.api.util.isElevated
import com.paraiso.api.util.withAdminAuth
import com.paraiso.api.util.withElevatedAuth
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.adminController(adminApi: AdminApi) {
    authenticate("auth0") {
        route("admin") {
            get("/userReports") {
                withElevatedAuth {
                    adminApi.getUserReports().let {
                        call.respond(HttpStatusCode.OK, it)
                    }
                }
            }
            get("/postReports") {
                withElevatedAuth {
                    adminApi.getPostReports().let {
                        call.respond(HttpStatusCode.OK, it)
                    }
                }
            }
        }
    }
}

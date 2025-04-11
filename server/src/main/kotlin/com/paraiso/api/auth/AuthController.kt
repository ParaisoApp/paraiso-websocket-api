package com.paraiso.com.paraiso.api.auth

import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.auth.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class AuthController {
    private val authApi = AuthApi()
    fun handleAuth(route: Route) {
        route.route("auth") {
            route.post {
                // fake auth controller
                authApi.getAuth(call.receive<String>())?.let { role ->
                    call.respond(HttpStatusCode.OK, role)
                }
            }
        }
    }
}

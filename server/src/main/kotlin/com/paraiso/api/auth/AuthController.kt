package com.paraiso.com.paraiso.api.auth

import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.messageTypes.Login
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authController(authApi: AuthApi) {
    route("auth") {
        post {
            // fake auth controller
//            authApi.getAuth(call.receive<Login>())?.let { role ->
                call.respond(HttpStatusCode.OK)
//            }
        }
    }
}

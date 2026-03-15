package com.paraiso.api.auth

import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.auth.AuthId
import com.paraiso.domain.auth.Login
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authController(authApi: AuthApi, config: HoconApplicationConfig) {
    route("auth") {
        post {
            // fake auth controller
            authApi.getAuth(call.receive<Login>()).let { role ->
                call.respond(HttpStatusCode.OK, role)
            }
        }
        post("syncUser") {// 1. Validate Shared Secret
            val receivedSecret = call.request.header("X-Internal-Secret")
            val expectedSecret = config.property("auth.internalSyncSecret").getString()

            if (receivedSecret != expectedSecret) {
                call.application.environment.log.warn("Unauthorized sync attempt from IP: ${call.request.origin.remoteHost}")
                call.respond(HttpStatusCode.Unauthorized, "Invalid secret")
            } else {
                authApi.syncUser(call.receive<AuthId>()).let { sync ->
                    call.respond(HttpStatusCode.OK, sync)
                }
            }
        }
    }
}

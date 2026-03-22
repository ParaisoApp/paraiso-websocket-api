package com.paraiso.api.auth

import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.auth.AuthId
import com.paraiso.domain.auth.AuthIdResponse
import com.paraiso.domain.auth.Login
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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
        post("syncUser") {// Validate Shared Secret
            val receivedSecret = call.request.header("X-Internal-Secret")
            val expectedSecret = config.property("auth.internalSyncSecret").getString()

            if (receivedSecret != expectedSecret) {
                call.application.environment.log.warn("Unauthorized sync attempt from IP: ${call.request.origin.remoteHost}")
                call.respond(HttpStatusCode.Unauthorized, "Invalid secret")
            } else {
                authApi.syncUser(call.receive<AuthIdResponse>()).let { sync ->
                    call.respond(HttpStatusCode.OK, sync)
                }
            }
        }
        authenticate("auth0"){
            post("ticket"){
                val principal = call.principal<JWTPrincipal>()
                val auth0Id = principal?.payload?.subject ?: return@post call.respond(HttpStatusCode.Unauthorized)
                authApi.ticket(auth0Id)?.let { ticket ->
                    call.respond(HttpStatusCode.OK, ticket)
                } ?: run { call.respond(HttpStatusCode.Unauthorized) }
            }
        }
    }
}

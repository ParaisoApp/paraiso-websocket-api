package com.paraiso.com.paraiso.api.auth

import com.paraiso.domain.auth.AuthApi

class AuthController {
    private val authApi = AuthApi()
//    fun handleAuth(route: Route) {
//        route.route("auth") {
//            route.post {
//                // fake auth controller
//                authApi.getAuth(call.receive<String>())?.let { role ->
//                    call.respond(HttpStatusCode.OK, role)
//                }
//            }
//        }
//    }
}

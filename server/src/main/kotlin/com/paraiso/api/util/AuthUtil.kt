package com.paraiso.api.util

import com.paraiso.domain.users.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.Serializable

@Serializable
data class UserCookie(
    val userId: String,
    val role: UserRole
)
fun UserCookie.isAdmin() = role == UserRole.ADMIN
fun UserCookie.isElevated() = role == UserRole.ADMIN || role == UserRole.MOD
suspend inline fun RoutingContext.withElevatedAuth(crossinline block: suspend () -> Unit) {
    val userCookie = call.sessions.get<UserCookie>()

    if (userCookie?.isElevated() == true) {
        block()
    } else {
        call.respond(HttpStatusCode.Unauthorized, "User not authorized to view reports")
    }
}
suspend inline fun RoutingContext.withAdminAuth(crossinline block: suspend () -> Unit) {
    val userCookie = call.sessions.get<UserCookie>()

    if (userCookie?.isAdmin() == true) {
        block()
    } else {
        call.respond(HttpStatusCode.Unauthorized, "User not authorized to view reports")
    }
}
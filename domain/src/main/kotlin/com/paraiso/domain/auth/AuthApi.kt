package com.paraiso.domain.auth

import com.paraiso.domain.users.CacheService
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.util.ServerConfig
import java.util.UUID

class AuthApi(
    private val usersApi: UsersApi,
    private val cacheService: CacheService
) {
    fun getAuth(login: Login): UserRole {
        return if (login.password == ServerConfig.admin) {
            UserRole.ADMIN
        } else {
            UserRole.GUEST
        }
    }
    suspend fun syncUser(authId: AuthId) =
        usersApi.syncUser(authId)
    suspend fun ticket(authId: String) =
        usersApi.findUserByAuthId(authId)?.let { user ->
            val ticket = UUID.randomUUID().toString()
            cacheService.set("ws_ticket:$ticket", authId, 60L)
            TicketResponse(ticket, user.id)
        }

}

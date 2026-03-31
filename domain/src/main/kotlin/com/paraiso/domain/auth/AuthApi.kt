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
    // link user to guest account that already exists on new
    suspend fun syncUser(authId: AuthId) =
        // check if user already created with this authId, otherwise use passed in ID
        usersApi.findUserByAuthId(authId.id).let { user ->
            if(user == null){
                usersApi.syncUser(authId)
            }
        }
    suspend fun ticket(authId: String) =
        usersApi.findUserByAuthId(authId)?.let { user ->
            val ticket = UUID.randomUUID().toString()
            cacheService.set("ws_ticket:$ticket", user.id, 60L)
            TicketResponse(ticket)
        }

}

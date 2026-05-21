package com.paraiso.domain.auth

import com.paraiso.domain.messageTypes.RoleUpdate
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.users.CacheService
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.util.ServerConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthApi(
    private val usersApi: UsersApi,
    private val postsApi: PostsApi,
    private val cacheService: CacheService
) {
    // link user to guest account that already exists on new
    suspend fun syncUser(authId: AuthId) = coroutineScope {
        // check if user already created with this authId, otherwise use passed in ID
        usersApi.findUserByAuthId(authId.id).let { user ->
            if(user == null){
                launch { usersApi.syncUser(authId) }
                //update the user role of all posts created by this user
                postsApi.setUserRole(RoleUpdate(authId.userId, UserRole.USER))
            }
        }
    }
    suspend fun ticket(authId: String) =
        usersApi.findUserByAuthId(authId)?.let { user ->
            val ticket = UUID.randomUUID().toString()
            cacheService.set("ws_ticket:$ticket", user.id, 60L)
            TicketResponse(ticket)
        }

}

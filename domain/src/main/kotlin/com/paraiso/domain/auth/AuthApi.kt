package com.paraiso.domain.auth

import com.paraiso.domain.messageTypes.Login
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.buildUserResponse
import com.paraiso.domain.util.ServerConfig
import com.paraiso.domain.util.ServerState

class AuthApi {
    suspend fun getAuth(login: Login): UserRole? {
        return if (login.password == ServerConfig.admin) {
            ServerState.userList[login.userId]?.let { user ->
                user.copy(
                    roles = UserRole.ADMIN,
                    name = "Breeze"
                ).let { admin ->
                    ServerState.userList[login.userId] = admin
                    ServerState.userUpdateFlowMut.emit(admin.buildUserResponse())
                }
            }
            UserRole.ADMIN
        } else {
            null
        }
    }
}

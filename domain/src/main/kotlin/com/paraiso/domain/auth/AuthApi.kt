package com.paraiso.domain.auth

import com.paraiso.domain.util.ServerConfig
import com.paraiso.domain.util.ServerState
import com.paraiso.domain.util.messageTypes.Login

class AuthApi {
    suspend fun getAuth(login: Login): UserRole? {
        return if (login.password == ServerConfig.admin) {
            ServerState.userList[login.userId]?.let{user ->
                user.copy(
                    roles = UserRole.ADMIN,
                    name = "Breeze"
                ).let{admin ->
                    ServerState.userList[login.userId] = admin
                    ServerState.userLoginFlowMut.emit(admin)

                }
            }
            UserRole.ADMIN
        } else {
            null
        }
    }
}

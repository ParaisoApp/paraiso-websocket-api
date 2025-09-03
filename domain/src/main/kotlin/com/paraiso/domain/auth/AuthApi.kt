package com.paraiso.domain.auth

import com.paraiso.domain.messageTypes.Login
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.ServerConfig

class AuthApi {
    fun getAuth(login: Login): UserRole {
        return if (login.password == ServerConfig.admin) {
            UserRole.ADMIN
        } else {
            UserRole.GUEST
        }
    }
}

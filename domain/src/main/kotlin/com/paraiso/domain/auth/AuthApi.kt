package com.paraiso.domain.auth

import com.paraiso.domain.util.ServerConfig

class AuthApi {
    fun getAuth(login: String): UserRole? {
        return if (login == ServerConfig.admin) {
            UserRole.ADMIN
        } else {
            null
        }
    }
}

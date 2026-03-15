package com.paraiso.domain.auth

import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UsersDB
import com.paraiso.domain.util.ServerConfig

class AuthApi(private val usersDB: UsersDB) {
    fun getAuth(login: Login): UserRole {
        return if (login.password == ServerConfig.admin) {
            UserRole.ADMIN
        } else {
            UserRole.GUEST
        }
    }
    suspend fun syncUser(authId: AuthId) {
        usersDB.syncUserAuth(authId)
    }
}

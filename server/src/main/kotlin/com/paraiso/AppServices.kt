package com.paraiso.com.paraiso

import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi

data class AppServices(
    val authApi: AuthApi,
    val adminApi: AdminApi,
    val postsApi: PostsApi,
    val routesApi: RoutesApi,
    val usersApi: UsersApi,
    val userSessionsApi: UserSessionsApi,
    val userChatsApi: UserChatsApi,
    val sportApi: SportApi,
    val metadataApi: MetadataApi
)

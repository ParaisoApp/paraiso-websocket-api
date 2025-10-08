package com.paraiso

import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.blocks.BlocksApi
import com.paraiso.domain.follows.FollowsApi
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.notifications.NotificationsApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.sport.sports.SportApi
import com.paraiso.domain.userchats.DirectMessagesApi
import com.paraiso.domain.userchats.UserChatsApi
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.domain.votes.VotesApi

data class AppServices(
    val authApi: AuthApi,
    val adminApi: AdminApi,
    val postsApi: PostsApi,
    val routesApi: RoutesApi,
    val usersApi: UsersApi,
    val votesApi: VotesApi,
    val followsApi: FollowsApi,
    val blocksApi: BlocksApi,
    val notificationsApi: NotificationsApi,
    val userSessionsApi: UserSessionsApi,
    val userChatsApi: UserChatsApi,
    val directMessagesApi: DirectMessagesApi,
    val sportApi: SportApi,
    val metadataApi: MetadataApi
)

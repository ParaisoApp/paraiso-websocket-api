package com.paraiso.com.paraiso

import com.paraiso.database.sports.AthletesDBAdapterImpl
import com.paraiso.database.sports.BoxscoresDBAdapterImpl
import com.paraiso.database.sports.CoachesDBAdapterImpl
import com.paraiso.database.sports.CompetitionsDBAdapterImpl
import com.paraiso.database.sports.LeadersDBAdapterImpl
import com.paraiso.database.sports.RostersDBAdapterImpl
import com.paraiso.database.sports.SchedulesDBAdapterImpl
import com.paraiso.database.sports.ScoreboardsDBAdapterImpl
import com.paraiso.database.sports.StandingsDBAdapterImpl
import com.paraiso.database.sports.TeamsDBAdapterImpl
import com.paraiso.domain.admin.AdminApi
import com.paraiso.domain.auth.AuthApi
import com.paraiso.domain.metadata.MetadataApi
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.sport.sports.bball.BBallApi
import com.paraiso.domain.sport.sports.fball.FBallApi
import com.paraiso.domain.users.UserChatsApi
import com.paraiso.domain.users.UserSessionsApi
import com.paraiso.domain.users.UsersApi
import com.paraiso.server.plugins.WebSocketHandler

data class AppServices(
    val authApi: AuthApi,
    val adminApi: AdminApi,
    val postsApi: PostsApi,
    val routesApi: RoutesApi,
    val usersApi: UsersApi,
    val userSessionsApi: UserSessionsApi,
    val userChatsApi: UserChatsApi,
    val bBallApi: BBallApi,
    val fBallApi: FBallApi,
    val metadataApi: MetadataApi
)

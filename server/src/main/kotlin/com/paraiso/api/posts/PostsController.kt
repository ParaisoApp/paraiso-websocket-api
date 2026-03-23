package com.paraiso.api.posts

import com.paraiso.api.util.UserCookie
import com.paraiso.domain.posts.PostSearch
import com.paraiso.domain.posts.PostSearchId
import com.paraiso.domain.posts.PostSearchIdRequest
import com.paraiso.domain.posts.PostSearchRequest
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.posts.toDomain
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Route.postsController(postsApi: PostsApi) {
    route("posts") {
        post {
            postsApi.getPosts(
                call.receive<PostSearchRequest>().toDomain(userId = call.sessions.get<UserCookie>()?.userId ?: "")
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("getById") {
            postsApi.getById(
                call.receive<PostSearchIdRequest>().toDomain(userId = call.sessions.get<UserCookie>()?.userId ?: "")
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.NoContent) }
        }
        post("getByIdsBasic") {
            postsApi.getByIdsBasic(
                call.sessions.get<UserCookie>()?.userId ?: "",
                call.receive<Set<String>>()
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("getByIds") {
            postsApi.getByIds(
                call.sessions.get<UserCookie>()?.userId ?: "",
                call.receive<Set<String>>(),
                call.request.queryParameters["sessionId"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        get("getByPartial") {
            postsApi.getByPartial(
                call.sessions.get<UserCookie>()?.userId ?: "",
                call.request.queryParameters["search"] ?: "",
                call.request.queryParameters["sessionId"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

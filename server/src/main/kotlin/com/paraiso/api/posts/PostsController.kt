package com.paraiso.com.paraiso.api.posts

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.PostsApi
import com.paraiso.domain.posts.Range
import com.paraiso.domain.posts.SortType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.postsController(postsApi: PostsApi) {
    route("posts") {
        post {
            postsApi.getPosts(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["name"] ?: "",
                call.request.queryParameters["range"]?.let { Range.valueOf(it) } ?: Range.DAY,
                call.request.queryParameters["sort"]?.let { SortType.valueOf(it) } ?: SortType.NEW,
                call.receive<FilterTypes>(),
                call.request.queryParameters["userId"] ?: ""
            ).let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
        post("getById") {
            postsApi.getById(
                call.request.queryParameters["id"] ?: "",
                call.request.queryParameters["range"]?.let { Range.valueOf(it) } ?: Range.DAY,
                call.request.queryParameters["sort"]?.let { SortType.valueOf(it) } ?: SortType.NEW,
                call.receive<FilterTypes>(),
                call.request.queryParameters["userId"] ?: ""
            )?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: run { call.respond(HttpStatusCode.InternalServerError) }
        }
        get("getByPartial") {
            postsApi.getByPartial(call.request.queryParameters["search"] ?: "").let {
                call.respond(HttpStatusCode.OK, it)
            }
        }
    }
}

package com.paraiso.domain.admin

import com.paraiso.domain.posts.PostReturn
import com.paraiso.domain.users.UserResponse

data class UserReports(
    val userReports: Set<UserReport>
)

data class UserReport(
    val user: UserResponse,
    val reportedBy: Set<String>
)

data class PostReports(
    val postReports: Set<PostReport>
)

data class PostReport(
    val post: PostReturn,
    val reportedBy: Set<String>
)

package com.paraiso.domain.admin

import com.paraiso.domain.posts.PostReturn
import com.paraiso.domain.users.UserResponse
import kotlinx.serialization.Serializable

@Serializable
data class UserReport(
    val user: UserResponse,
    val reportedBy: Set<String>
)

@Serializable
data class PostReport(
    val post: PostReturn,
    val reportedBy: Set<String>
)

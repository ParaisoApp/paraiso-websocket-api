package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.users.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class FilterTypes(
    val postTypes: Set<PostType>,
    val userRoles: Set<UserRole>
)
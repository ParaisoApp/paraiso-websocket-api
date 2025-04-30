package com.paraiso.com.paraiso.server.util

import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.users.UserRole

class SessionState {
    val sortType = SortType.New
    val postTypes = setOf(
        PostType.COMMENT,
        PostType.SUB
    )
    val userTypes = setOf(
        UserRole.USER,
        UserRole.GUEST,
    )
}

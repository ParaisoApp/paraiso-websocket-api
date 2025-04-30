package com.paraiso.com.paraiso.server.util

import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.users.UserRole

class SessionState {
    var filterTypes = FilterTypes(
        postTypes = setOf(
            PostType.COMMENT,
            PostType.SUB
        ),
        userRoles = setOf(
            UserRole.USER,
            UserRole.GUEST
        )
    )
    val sortType = SortType.New
}

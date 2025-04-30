package com.paraiso.com.paraiso.server.util

import com.paraiso.domain.posts.FilterType
import com.paraiso.domain.posts.SortType

class SessionState {
    val sortType = SortType.New
    val filterTypes = setOf(
        FilterType.Posts,
        FilterType.Comments,
        FilterType.Users,
        FilterType.Guests
    )
}

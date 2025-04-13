package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Delete(
    val userId: String,
    val postId: String,
    val parentId: String
)

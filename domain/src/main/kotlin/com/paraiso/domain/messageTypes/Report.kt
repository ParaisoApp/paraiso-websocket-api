package com.paraiso.domain.messageTypes

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String // can be post or userId
)

package com.paraiso.domain.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Metadata(
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val video: String? = null,
    val url: String
)

package com.paraiso.domain.links

import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val link: Map<String, String>
)

package com.paraiso.database.links

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    @SerialName(ID) val id: String,
    val type: LinkType,
    val value: String
)

@Serializable
enum class LinkType {
    @SerialName("DONATE")
    DONATE,

    @SerialName("SOCIAL")
    SOCIAL,
}

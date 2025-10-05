package com.paraiso.client.metadata

import com.paraiso.domain.metadata.Metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RestMetadata(
    val title: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_url") val authorUrl: String? = null,
    val type: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("provider_url") val providerUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val html: String? = null
)

fun RestMetadata.toDomain(url: String) = Metadata(
    title = title,
    description = "",
    image = thumbnailUrl,
    video = extractEmbedUrl(html),
    url = url
)

private fun extractEmbedUrl(html: String?): String? {
    if (html == null) return null

    val regex = Regex("""src=["']([^"']+)["']""")
    return regex.find(html)?.groups?.get(1)?.value
}

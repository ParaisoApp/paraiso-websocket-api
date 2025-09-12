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

enum class Platform(val regex: Regex, val oembedEndpoint: String) {
    YOUTUBE(
        Regex("https?://(www\\.|m\\.)?youtube\\.com/watch.*"),
        "https://www.youtube.com/oembed"
    ),
    TWITTER(
        Regex("https?://(www\\.|mobile\\.)?twitter\\.com/.+/status/\\d+"),
        "https://publish.twitter.com/oembed"
    ),
    TWITCH(
        Regex("https?://(www\\.)?twitch\\.tv/.*"),
        "https://api.twitch.tv/v5/oembed?url="
    ),
    STREAMABLE(
        Regex("https?://(www\\.)?streamable\\.com/.*"),
        "https://api.streamable.com/oembed.json"
    ),
    REDDIT(
        Regex("https?://(www\\.|m\\.)?reddit\\.com/r/.+/comments/.*"),
        "https://www.reddit.com/oembed"
    )
}

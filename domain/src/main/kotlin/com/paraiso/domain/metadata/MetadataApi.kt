package com.paraiso.domain.metadata

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MetadataApi {
    suspend fun getMetadata(url: String): Metadata? = coroutineScope {
        // First, do a GET request to check headers
        val response = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36")
            .referrer("https://www.google.com")
            .ignoreContentType(true)
            .timeout(15_000)
            .execute()

        val contentType = response.contentType()

        if (contentType?.startsWith("image/") == true) {
            // Direct image URL
            Metadata(
                image = url,
                url = url
            )
        } else {
            // Parse HTML
            val doc: Document = response.parse()

            val title = firstAvailable(
                meta("property", "og:title", doc),
                meta("name", "twitter:title", doc),
                doc.select("title").text()
            )

            val description = firstAvailable(
                meta("property", "og:description", doc),
                meta("name", "description", doc),
                meta("name", "twitter:description", doc)
            )

            val image = firstAvailable(
                meta("property", "og:image", doc),
                meta("name", "twitter:image", doc),
                doc.select("link[rel=image_src]").attr("href"),
                doc.select("img").firstOrNull()?.absUrl("src")
            )

            val video = firstAvailable(
                meta("property", "og:video", doc),
                meta("name", "twitter:player", doc),
                doc.select("video source").firstOrNull()?.absUrl("src"),
                doc.select("iframe").firstOrNull()?.absUrl("src")
            )

            Metadata(
                title = title,
                description = description,
                image = image,
                video = video,
                url = url
            )
        }
    }
    private fun meta(attr: String, key: String, doc: Document): String? =
        doc.select("meta[$attr=$key]").firstOrNull()?.attr("content")
    private fun firstAvailable(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }
}

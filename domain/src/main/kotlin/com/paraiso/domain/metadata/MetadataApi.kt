package com.paraiso.domain.metadata

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
class MetadataApi {
    private fun meta(attr: String, key: String, doc: Document): String? =
        doc.select("meta[$attr=$key]").firstOrNull()?.attr("content")
    private fun firstAvailable(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }
    suspend fun getMetadata(url: String): Metadata? = coroutineScope {
        Jsoup.connect(url)
            .ignoreContentType(true)
            .method(Connection.Method.HEAD)
            .execute()
            .contentType()?.let { contentType ->
                if (contentType.startsWith("image")) {
                    Metadata(
                        image = url,
                        url = url
                    )
                } else if (contentType.startsWith("text")) {
                    val doc: Document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10_000)
                        .get()

                    val title = async {
                        firstAvailable(
                            meta("property", "og:title", doc),
                            meta("name", "twitter:title", doc),
                            meta("name", "title", doc),
                            doc.select("title").text()
                        )
                    }

                    val description = async {
                        firstAvailable(
                            meta("property", "og:description", doc),
                            meta("name", "description", doc),
                            meta("name", "twitter:description", doc)
                        )
                    }

                    val image = async {
                        firstAvailable(
                            meta("property", "og:image", doc),
                            meta("name", "twitter:image", doc),
                            doc.select("link[rel=image_src]").attr("href"),
                            doc.select("img").firstOrNull()?.absUrl("src")
                        )
                    }

                    val video = async {
                        firstAvailable(
                            meta("property", "og:video", doc),
                            meta("name", "twitter:player", doc),
                            doc.select("video source").firstOrNull()?.absUrl("src"),
                            doc.select("iframe").firstOrNull()?.absUrl("src")
                        )
                    }

                    Metadata(
                        title = title.await(),
                        description = description.await(),
                        image = image.await(),
                        video = video.await(),
                        url = url
                    )
                } else { null }
            } ?: run { null }
    }
}

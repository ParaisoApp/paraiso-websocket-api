package com.paraiso.client.metadata

import com.paraiso.client.util.BaseAdapter
import com.paraiso.domain.metadata.Metadata
import com.paraiso.domain.metadata.MetadataClient
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataClientImpl: MetadataClient, BaseAdapter, Klogging {
    companion object {
        private val dispatcher = Dispatchers.IO
    }
    override suspend fun getMetadata(url: String, baseUrl: String): Metadata? = withContext(dispatcher){
        try {
            val response: RestMetadata = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(baseUrl)
        } catch (e: Exception) {
            println("Failed to fetch oEmbed for $url: ${e.message}")
            null
        }
    }
}
package com.paraiso.domain.metadata

interface MetadataClient {
    suspend fun getMetadata(url: String, baseUrl: String): Metadata?
}

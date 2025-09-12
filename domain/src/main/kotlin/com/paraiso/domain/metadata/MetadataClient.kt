package com.paraiso.domain.metadata

import com.paraiso.domain.routes.SiteRoute

interface MetadataClient {
    suspend fun getMetadata(url: String, baseUrl: String): Metadata?
}

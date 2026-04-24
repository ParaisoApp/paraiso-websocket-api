package com.paraiso.domain.links

interface LinksDB {
    suspend fun findLinksByType(type: String): Map<String, String>
}
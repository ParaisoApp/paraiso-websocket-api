package com.paraiso.domain.links

class LinksApi(private val linksDB: LinksDB) {
    suspend fun findLinksByType(type: String) = linksDB.findLinksByType(type)
}
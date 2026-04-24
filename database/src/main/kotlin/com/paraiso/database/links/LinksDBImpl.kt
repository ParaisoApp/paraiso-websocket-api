package com.paraiso.database.links

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.blocks.Block
import com.paraiso.domain.links.LinksDB
import io.klogging.Klogging
import kotlinx.coroutines.flow.toList

class LinksDBImpl(database: MongoDatabase) : LinksDB, Klogging {
    private val collection = database.getCollection("links", Link::class.java)
    override suspend fun findLinksByType(type: String): Map<String, String> =
        try{
            collection.find(
                Filters.eq(Link::type.name, type),
            ).toList().associate { it.id to it.value }
        } catch (ex: Exception){
            logger.error { "Error getting all links: $ex" }
            emptyMap()
        }
}
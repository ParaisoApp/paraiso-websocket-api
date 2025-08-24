package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.BoxScore

interface BoxscoresDB {
    suspend fun findById(id: String): BoxScore?
    suspend fun findByIdsIn(ids: List<String>): List<BoxScore>
    suspend fun save(boxscores: List<BoxScore>): Int
}

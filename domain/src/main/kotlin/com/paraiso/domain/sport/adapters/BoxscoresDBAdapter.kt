package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.BoxScore

interface BoxscoresDBAdapter {
    suspend fun findById(id: String): BoxScore?
    suspend fun findByIdsIn(ids: List<String>): List<BoxScore>
    suspend fun save(boxscores: List<BoxScore>): Int
}

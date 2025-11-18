package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.ScoreboardEntity

interface ScoreboardsDB {
    suspend fun findById(id: String): ScoreboardEntity?
    suspend fun save(scoreboards: List<ScoreboardEntity>): Int
}

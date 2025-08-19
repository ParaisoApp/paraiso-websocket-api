package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.ScoreboardEntity

interface ScoreboardsDBAdapter {
    suspend fun findById(id: String): ScoreboardEntity?
    suspend fun save(scoreboards: List<ScoreboardEntity>): Int
}

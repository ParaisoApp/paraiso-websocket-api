package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.ScoreboardEntity

interface ScoreboardsDB {
    suspend fun findById(id: String): ScoreboardEntity?
    suspend fun findScoreboard(
        sport: String,
        year: String,
        type: String,
        modifier: String,
        past: Boolean
    ): ScoreboardEntity?
    suspend fun save(scoreboards: List<ScoreboardEntity>): Int
}

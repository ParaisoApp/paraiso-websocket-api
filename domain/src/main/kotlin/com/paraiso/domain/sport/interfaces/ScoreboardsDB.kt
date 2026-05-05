package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Scoreboard

interface ScoreboardsDB {
    suspend fun findByIdIn(ids: List<String>): List<Scoreboard>
    suspend fun save(scoreboards: List<Scoreboard>): Int
}

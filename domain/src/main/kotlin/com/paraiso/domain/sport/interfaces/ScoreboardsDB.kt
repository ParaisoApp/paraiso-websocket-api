package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Scoreboard

interface ScoreboardsDB {
    suspend fun findById(id: String): Scoreboard?
    suspend fun save(scoreboards: List<Scoreboard>): Int
}

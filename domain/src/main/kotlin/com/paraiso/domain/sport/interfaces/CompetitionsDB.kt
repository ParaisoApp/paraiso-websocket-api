package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Competition

interface CompetitionsDB {
    suspend fun findById(id: String): Competition?
    suspend fun findByIdIn(ids: Set<String>): List<Competition>
    suspend fun save(competitions: List<Competition>): Int
    suspend fun saveIfNew(competitions: List<Competition>): Int
    suspend fun findScoreboard(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ): List<Competition>

    suspend fun findPlayoffsByYear(
        sport: String,
        year: Int
    ): List<Competition>
}

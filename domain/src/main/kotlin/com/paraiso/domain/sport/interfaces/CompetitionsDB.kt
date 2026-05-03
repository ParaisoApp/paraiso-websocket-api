package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Competition

interface CompetitionsDB {
    suspend fun findByIdIn(ids: Set<String>): List<Competition>
    suspend fun findByTeamsAndNotStarted(teamIds: List<String>): List<String>
    suspend fun save(competitions: List<Competition>): Int
    suspend fun saveWithExisting(competitions: List<Competition>): Int
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
    suspend fun setCompsDeleted(ids: List<String>): Long
}

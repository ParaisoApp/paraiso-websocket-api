package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.Competition

interface CompetitionsDBAdapter{
    suspend fun findById(id: String): Competition?
    suspend fun findByIdIn(ids: List<String>): List<Competition>
    suspend fun save(competitions: List<Competition>): Int

}

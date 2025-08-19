package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.Athlete

interface AthletesDBAdapter {
    suspend fun findByIdsIn(ids: List<String>): List<Athlete>
    suspend fun save(athletes: List<Athlete>): Int
}

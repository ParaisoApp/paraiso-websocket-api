package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Athlete

interface AthletesDB {
    suspend fun findByIdsIn(ids: List<String>): List<Athlete>
    suspend fun save(athletes: List<Athlete>): Int
}

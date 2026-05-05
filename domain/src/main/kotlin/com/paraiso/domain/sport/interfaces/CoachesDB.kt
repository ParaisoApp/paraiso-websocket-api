package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Coach

interface CoachesDB {
    suspend fun findByIdIn(ids: List<String>): List<Coach>
    suspend fun save(coaches: List<Coach>): Int
}

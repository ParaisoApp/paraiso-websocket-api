package com.paraiso.domain.sport.interfaces

import com.paraiso.domain.sport.data.Coach

interface CoachesDB {
    suspend fun findById(id: String): Coach?
    suspend fun save(coaches: List<Coach>): Int
}

package com.paraiso.domain.admin

interface UserReportsDBAdapter{
    suspend fun getAll(): List<UserReport>
    suspend fun save(userReports: List<UserReport>): Int
    suspend fun addUserReport(
        userId: String,
        reportingUserId: String
    ): Long
}

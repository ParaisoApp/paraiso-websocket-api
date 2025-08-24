package com.paraiso.domain.admin

interface PostReportsDB {
    suspend fun getAll(): List<PostReport>
    suspend fun save(postReports: List<PostReport>): Int
    suspend fun addPostReport(
        postId: String,
        reportingUserId: String
    ): Long
}

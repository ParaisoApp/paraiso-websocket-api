package com.paraiso.domain.admin

interface PostReportsDBAdapter {
    suspend fun getAll(): List<PostReport>
    suspend fun save(postReports: List<PostReport>): Int
    suspend fun addPostReport(
        postId: String,
        reportingUserId: String
    ): Long
}

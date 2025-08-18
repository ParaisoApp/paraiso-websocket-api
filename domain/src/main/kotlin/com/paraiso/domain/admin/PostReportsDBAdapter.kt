package com.paraiso.domain.admin

interface PostReportsDBAdapter {
    suspend fun getAll(): List<PostReport>
    suspend fun save(postReports: List<PostReport>): List<String>
    suspend fun addPostReport(
        postId: String,
        reportingUserId: String
    ): Long
}

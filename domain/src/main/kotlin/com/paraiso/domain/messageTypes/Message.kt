package com.paraiso.domain.messageTypes

import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.ActiveStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.util.RecordSerializer
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

const val COMMENT_WEIGHTING = 0.5
const val EPOCH_OFFSET = 1735689600000L // Jan 1, 2025

// Controls how much a single vote is worth relative to time
// Lower values = time dominates more (faster decay). Higher values = votes dominate more.
const val HOT_VOTE_BOOST = 5.0
const val RISING_VOTE_BOOST = 12.0

@Serializable
data class Message(
    val id: String? = null,
    val userId: String?,
    @Serializable(with = RecordSerializer::class)
    val userReceiveIds: Set<String>,
    val title: String?,
    val content: String?,
    val type: PostType,
    val media: String?,
    val data: String?,
    val replyId: String?,
    val rootId: String?,
    val editId: String?,
    val route: String,
    val userRole: UserRole
): ServerEvent

fun Message.toNewPost() =
    Clock.System.now().let { now ->
        val (topScore, hotScore, risingScore) = calculateScores(1, 0, now.toEpochMilliseconds())
        Post(
            id = id,
            userId = userId,
            userRole = userRole,
            title = title,
            content = content,
            type = type,
            media = media,
            votes = 1,
            count = 0,
            topScore = topScore,
            hotScore = hotScore,
            risingScore = risingScore,
            parentId = replyId,
            rootId = rootId,
            status = ActiveStatus.ACTIVE,
            data = data,
            route = route,
            userVote = null,
            tags = emptySet(),
            createdOn = now,
            updatedOn = now
        )
    }

fun calculateScores(voteSum: Int, commentCount: Int, createdOnMs: Long): Triple<Double, Double, Double> {
    val rawEngagement = voteSum + (COMMENT_WEIGHTING * commentCount)
    val topScore = max(0.0, rawEngagement.toDouble())

    // Time Factor grows infinitely into the future.
    // Dividing by 3,600,000 means the baseline increases by exactly 1.0 every hour.
    val timeFactor = (createdOnMs - EPOCH_OFFSET) / (1000.0 * 60 * 60)

    // Engagement multiplier using a gentle log
    // We add 1.0 so log10(1) = 0 when there's no extra engagement.
    val engagementMultiplier = log10(topScore + 1.0)

    // Brand new posts with 1 vote get exactly the timeFactor.
    // Posts with high engagement get a massive push forward
    val hotScore = timeFactor + (engagementMultiplier * HOT_VOTE_BOOST)
    val risingScore = timeFactor + (engagementMultiplier * RISING_VOTE_BOOST)

    return Triple(topScore, hotScore, risingScore)
}

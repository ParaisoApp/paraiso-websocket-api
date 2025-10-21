package com.paraiso.database.votes

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.users.User
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.votes.Vote
import com.paraiso.domain.votes.VotesDB
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class VotesDBImpl(database: MongoDatabase) : VotesDB {

    private val collection = database.getCollection("votes", Vote::class.java)

    override suspend fun findByVoterIdAndPostId(voterId: String, postId: String) =
        collection.find(
            and(
                eq(Vote::voterId.name, voterId),
                eq(Vote::postId.name, postId)
            )
        ).limit(1).firstOrNull()

    override suspend fun findByUserIdAndPostIdIn(userId: String, postIds: Set<String>) =
        collection.find(
            and(
                eq(Vote::voterId.name, userId),
                `in`(Vote::postId.name, postIds)
            )
        ).toList()

    override suspend fun save(votes: List<Vote>): Int {
        val bulkOps = votes.map { vote ->
            ReplaceOneModel(
                eq(ID, vote.id),
                vote,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun setVote(voterId: String, postId: String, vote: Boolean) =
        collection.updateOne(
            and(
                eq(Vote::voterId.name, voterId),
                eq(Vote::postId.name, postId)
            ),
            combine(
                set(Vote::upvote.name, vote),
                set(User::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount

    override suspend fun delete(voterId: String, postId: String) =
        collection.deleteOne(
            and(
                eq(Vote::voterId.name, voterId),
                eq(Vote::postId.name, postId)
            )
        ).deletedCount
}

package com.paraiso.database.votes

import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.not
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.util.eqId
import com.paraiso.domain.messageTypes.FilterTypes
import com.paraiso.domain.messageTypes.Message
import com.paraiso.domain.messageTypes.Vote
import com.paraiso.domain.posts.Post
import com.paraiso.domain.posts.PostStatus
import com.paraiso.domain.posts.PostType
import com.paraiso.domain.posts.PostsDB
import com.paraiso.domain.posts.SortType
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UserRole
import com.paraiso.domain.users.UserSettings
import com.paraiso.domain.util.Constants.BASKETBALL_PREFIX
import com.paraiso.domain.util.Constants.FOOTBALL_PREFIX
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.Constants.USER_PREFIX
import com.paraiso.domain.votes.VotesDB
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bson.BsonDateTime
import org.bson.Document
import org.bson.conversions.Bson
import java.util.Date
import kotlin.time.Duration.Companion.days

class VotesDBImpl(database: MongoDatabase) : VotesDB {

    private val collection = database.getCollection("votes", Vote::class.java)

    override suspend fun findByVoterIdAndPostId(voterId: String, postId: String) =
        collection.find(
            and(
                eq(Vote::voterId.name, voterId),
                eq(Vote::postId.name, postId),
            )
        ).firstOrNull()

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
                eq(Vote::postId.name, postId),
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
                eq(Vote::postId.name, postId),
            )
        ).deletedCount
}
